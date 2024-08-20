package com.OLP.books.controller;

import com.OLP.common.entity.MqConstants;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import com.OLP.common.dto.BookDto;
import com.OLP.common.pojo.Book;
import com.OLP.common.pojo.BookEs;
import com.OLP.common.pojo.User;
import com.OLP.common.redis.RedisUtil;
import com.OLP.common.vo.BooksearchVo;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.OLP.books.common.R;
import com.OLP.books.common.exception.DataException;
import com.OLP.books.service.BookService;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.OLP.common.entity.SystemConstants.PAGE_SELECT_NONAME;

/**
 * 书籍搜索推荐功能
 */
@Slf4j
@RestController
@RequestMapping("/book")
public class BookController {
    @Autowired
    private BookService bookService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RestHighLevelClient client;

    @Resource
    private RabbitTemplate rabbitTemplate;
    //本地缓存
    private Cache<String,String> localCache =
            CacheBuilder.newBuilder()
                    .maximumSize(5000)
                    .expireAfterWrite(30,TimeUnit.SECONDS)
                    .build();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 分类下拉菜单展示
     * 设置guava本地缓存
     * @return
     */
    @GetMapping("/bookSortList")
    public R<BooksearchVo> sortlist(){
        String cacheKey = "BooksSortList.AllSorts";
        String content = localCache.getIfPresent(cacheKey);
        List<String> BookSortList = new ArrayList<>();
        if (StringUtils.isBlank(content)){
            BookSortList =  getBookSortList();
            //缓存为空，设置
            localCache.put(cacheKey, JSON.toJSONString(BookSortList));
        }else{
            BookSortList = JSON.parseArray(content,String.class);
        }
        BooksearchVo booksearchVo = new BooksearchVo();
        booksearchVo.setBookSortList(BookSortList);
        return R.success(booksearchVo);
    }

    private List<String> getBookSortList() {
        List<String> sortList = bookService.sortlist();
        return sortList;
    }

    /**
     * 点金首页默认展示状态
     * 或只选中种类
     * @param type
     * @return
     */
    @GetMapping("/common")
    public R<List<Book>> commonOrSort(String type){
        List<Book> books = bookService.commonSort(type);
        return R.success(books);
    }


    /**
     * 书籍搜索
     * 含种类加搜索内容筛选
     * @return
     */
    @PostMapping("/booksearch")
    public R<List<Book>> search(@RequestBody BookDto bookDto){
        List<Book> books = bookService.search(bookDto);
        return R.success(books);
    }


    /**
     * 待审核书籍展示
     * @return
     */
    @PostMapping("/Auditbooks")
    public R<List<Book>> AuditBooks(@RequestBody BookDto bookDto){
        List<Book> books = bookService.Auditsearch(bookDto);
        return R.success(books);
    }

    /**
     * 审核
     * @param bookId
     * @param isAudit
     * @return
     */
    @GetMapping("/auditCheck")
    public R<String> auditCheck(@RequestParam Long bookId,@RequestParam Boolean isAudit){
        Book book = bookService.getById(bookId);
        //第一次删
        redisUtil.deleteKeysBypattern(PAGE_SELECT_NONAME);
        if (isAudit){
            book.setStatus("1");
            bookService.updateById(book);
            //延迟双删
            long delayMillis = 5000; // 延迟5秒
            scheduleDelayedDelete(PAGE_SELECT_NONAME,delayMillis);
            //发送消息同步数据(增加)
            rabbitTemplate.convertAndSend(MqConstants.BOOKS_EXCHANGE,MqConstants.BOOKS_INSERTORUPDATE_KEY,bookId);
        } else {
            book.setStatus("2");
            bookService.updateById(book);
            //延迟双删
            long delayMillis = 5000; // 延迟5秒
            scheduleDelayedDelete(PAGE_SELECT_NONAME,delayMillis);
            //发送消息同步数据(删除)
            rabbitTemplate.convertAndSend(MqConstants.BOOKS_EXCHANGE,MqConstants.BOOKS_DELETE_KEY,bookId);
        }
        return  R.success("审核成功！");
    }


    private void scheduleDelayedDelete(String pattern, long delayMillis) {
        scheduler.schedule(() -> {
            log.info("延迟删除缓存: {}", pattern);
            redisUtil.deleteKeysBypattern(pattern);
        }, delayMillis, TimeUnit.MILLISECONDS);
    }


    /**
     * 下架
     * @param bookId
     * @param isTokenOff
     * @return
     */
    @GetMapping("/tokenOffshelves")
    public R<String> tokenOffshelves(@RequestParam Long bookId,@RequestParam Boolean isTokenOff){
        Book book = bookService.getById(bookId);
        if (book==null){
            throw new DataException("下架的书籍不存在！");
        }
        if (isTokenOff) {
            //删除缓存
            redisUtil.deleteKeysBypattern(PAGE_SELECT_NONAME);
            book.setStatus("2");
            bookService.updateById(book);
            //发送消息同步数据(删除)
            rabbitTemplate.convertAndSend(MqConstants.BOOKS_EXCHANGE,MqConstants.BOOKS_DELETE_KEY,bookId);
        }
        return  R.success("下架成功！");
    }

    /**
     * 通过select选择器分页展示
     * @param page
     * @param pageSize
     * @param type
     * @param date
     * @return
     */
    @GetMapping("/bookPagesearch")
    public R<Page> searchPage(int page, int pageSize, String type,@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date){
        //构造分页构造器
        Page pageinfo = new Page(page, pageSize);//page当前页
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(type)) {
            booklam.eq(Book::getCategory,type);
        }
        if(date != null) {
            booklam.eq(Book::getPublicationDate,date);
        }
        booklam.eq(Book::getStatus,"1").orderByDesc(Book::getPublicationDate);
        bookService.page(pageinfo,booklam);
        return R.success(pageinfo);
    }

    /**
     * 点击书籍进入详情
     * @return
     */
    @GetMapping("/booksClickSearch")
    public R<Book> booksClickSearch(Long bookId){
        Book book = bookService.getById(bookId);
        if (book==null){
            throw new DataException("没有该书籍");
        }
        return R.success(book);
    }



    /**
     * 分页展示
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(@RequestParam("page") int page,
                        @RequestParam("pageSize") int pageSize,
                        @RequestParam(value = "name", required = false) String name,
                        @RequestParam(value = "category", required = false) String category,
                        @RequestParam(value = "sdate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate sdate,
                        @RequestParam(value = "edate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate edate) {
        log.info("page = {},pageSize = {},name = {},sdate = {},edate={}", page, pageSize, name,sdate,edate);
        Page<Book> pagelist = null;
        //1.name和date都为空进入全部数据查询
        if (StringUtils.isBlank(name) && StringUtils.isBlank(category)  && sdate == null && edate == null &&page == 1 ) {
            String listjson =  redisUtil.get(PAGE_SELECT_NONAME +  pageSize);
            pagelist = new Gson().fromJson(listjson, new TypeToken<Page<Book>>() {}.getType());
            //1.1判断缓存是否存在
            if (pagelist != null) {
                //1.1.1存在直接返回
                return R.success(pagelist);
            }
            //1.1.2不存在查询数据库
            //自定义impl方法，顺便缓存到redis
            R r = bookService.getList(page, pageSize);
            return r;
        }
        //2.name不空进入全文检索
        //2.1
        //2.2进入自定义impl方法查询数据库
        R r = bookService.getEsPage(page, pageSize, name,category, sdate, edate);
        return r;
    }



    /**
     * 批量添加数据
     * @param key
     * @return
     * @throws IOException
     */
    @GetMapping("/test")
    public R<String> EStest(String key) throws IOException {
        //1.查询
        List<Book> list = bookService.list();
        //2.添加数据到索引库
        BulkRequest request = new BulkRequest();
        for (Book book : list) {
            BookEs bookEs = new BookEs(book);
            request.add(new IndexRequest("books")
                    .id(bookEs.getBookId().toString())
                    .source(JSON.toJSONString(bookEs), XContentType.JSON));
        }
        //3.发送请求
        client.bulk(request, RequestOptions.DEFAULT);
        return R.success("批量添加成功");
    }

    /**
     * 自动补全
     * @param key
     * @return
     * @throws IOException
     */
    @GetMapping("/suggestion")
    public R<List<String>> ESsuggestion(String key)  {
        try {
            //1.查询
            SearchRequest request = new SearchRequest("books");

            request.source()
                    .suggest(new SuggestBuilder().addSuggestion(
                            "mySuggestion",
                            SuggestBuilders
                                    .completionSuggestion("suggestion")
                                    .prefix(key)
                                    .skipDuplicates(true)
                                    .size(7)
                    ));

            //2.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            //3.解析结果
            Suggest suggest = response.getSuggest();
            //4.1根据名称获取补全结果
            CompletionSuggestion completionSuggestion =  suggest.getSuggestion("mySuggestion");
            List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getOptions();
            List<String> list = new ArrayList<>(options.size());
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().string();
                list.add(text);
            }
            return R.success(list);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
