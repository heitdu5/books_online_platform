package com.OLP.books.service.impl;

import com.OLP.books.common.R;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import com.OLP.common.dto.BookDto;
import com.OLP.common.pojo.Book;
import com.OLP.common.pojo.BookEs;
import com.OLP.common.redis.RedisUtil;
import com.OLP.common.util.LoginUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.OLP.books.common.exception.DataException;
import com.OLP.books.mapper.BookMapper;
import com.OLP.books.service.BookService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.OLP.common.entity.SystemConstants.PAGE_SELECT_NONAME;


@Service
@Slf4j
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
    @Autowired
    private BookMapper bookMapper;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RestHighLevelClient client;



    @Override
    public List<String> sortlist() {
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        booklam.eq(Book::getStatus, "1");
        List<Book> list = this.list();
        List<String> stringList = list.stream()
                .map(Book::getCategory)
                .distinct()  //去重
                .collect(Collectors.toList());
        return stringList;
    }


    @Override
    public List<Book> search(BookDto bookDto) {
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        //如果存在条件就筛选
        if (StringUtils.isNotBlank(bookDto.getType())) {
            booklam.eq(Book::getCategory, bookDto.getType());
        }

        if (StringUtils.isNotBlank(bookDto.getAuthor())) {
            booklam.like(Book::getAuthor, bookDto.getAuthor());
        }
        if (bookDto.getDate() != null) {
            booklam.eq(Book::getPublicationDate, bookDto.getDate());
        }
        //只展示已发布的书籍
        booklam.eq(Book::getStatus, "1");

        List<Book> list = this.list(booklam);
        if (list == null || list.size() == 0) {
            log.info("查询到的书籍为空");
            throw new DataException("查询的书籍不存在");
        }
        return list;
    }

    @Override
    public List<Book> commonSort(String type) {
        String status = "1";
        if (!StringUtils.isNotBlank(type)) {
            List<Book> books = bookMapper.displayAll(status);
            return books;
        }

        List<Book> books = bookMapper.displaysort(type, status);
        return books;
    }

    /**
     * 待审核的书籍
     *
     * @return
     */
    @Override
    public List<Book> Auditsearch(BookDto bookDto) {
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        //如果存在条件就筛选
        if (StringUtils.isNotBlank(bookDto.getType())) {
            booklam.eq(Book::getCategory, bookDto.getType());
        }

        if (StringUtils.isNotBlank(bookDto.getAuthor())) {
            booklam.like(Book::getAuthor, bookDto.getAuthor());
        }
        if (bookDto.getDate() != null) {
            booklam.eq(Book::getPublicationDate, bookDto.getDate());
        }
        //只展示已发布或者待审核的书籍
        booklam.in(Book::getStatus, "0", "1").orderByDesc(Book::getPublicationDate);

        List<Book> list = this.list(booklam);
        if (list == null || list.size() == 0) {
            log.info("查询到的书籍为空");
            throw new DataException("查询的书籍不存在");
        }
        return list;
    }

    @Override
    public R getList(int page, int pageSize) {
//        Page<Book> pageinfo = new Page(page, pageSize);
//        LambdaQueryWrapper<Book> booklambda = new LambdaQueryWrapper<>();
//        booklambda.eq(Book::getStatus, "1").orderByDesc(Book::getPublicationDate);
//        booklambda.orderByDesc(Book::getPublicationDate);
//        this.page(pageinfo, booklambda);
        try {
            //1.准备Request
            SearchRequest request = new SearchRequest("books");
            // 2.1.准备Boolean查询
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            //3.只放入审核通过的书籍
            boolQuery.must(QueryBuilders.termQuery("status","1"));

            request.source().query(boolQuery);
            //4.分页
            request.source().from((page - 1) * pageSize).size(pageSize);
            //5.排序
            SortBuilder sortBuilder = SortBuilders.fieldSort("publicationDate").order(SortOrder.DESC);
            request.source().sort(sortBuilder);
            //6.请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //7.解析响应
            List<Book> books = handleResponse(response);

            //8.分页构造
            //构造分页构造器
            Page<Book> pageinfo = new Page(page, pageSize);
            //9.要注意设置total值不然会有bug
            pageinfo.setTotal(response.getHits().getTotalHits().value);
            pageinfo.setRecords(books);
            redisUtil.setNx(PAGE_SELECT_NONAME + pageSize, new Gson().toJson(pageinfo), 48L, TimeUnit.HOURS);
            return R.success(pageinfo);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public R getEsPage(int page, int pageSize, String name, String categoty, LocalDate sdate, LocalDate edate) {

        try {
            //1.准备Request
            SearchRequest request = new SearchRequest("books");
            //2.query
            // 2.1.准备Boolean查询
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            //关键字
            if (StringUtils.isNotBlank(name)) {
                boolQuery.must(QueryBuilders.matchQuery("all", name));
            }

            //分类
            if (StringUtils.isNotBlank(categoty)) {
                boolQuery.must(QueryBuilders.termQuery("category", categoty));
            }
            //时间
            if (sdate != null && edate != null) {
                boolQuery.must(QueryBuilders.rangeQuery("publicationDate").gte(sdate).lte(edate));
            }
            //3.只放入审核通过的书籍
            boolQuery.must(QueryBuilders.termQuery("status","1"));

            request.source().query(boolQuery);
            //4.分页
            request.source().from((page - 1) * pageSize).size(pageSize);
            //5.排序
            SortBuilder sortBuilder = SortBuilders.fieldSort("publicationDate").order(SortOrder.DESC);
            request.source().sort(sortBuilder);
            //6.请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //7.解析响应
            List<Book> books = handleResponse(response);

            //8.分页构造
            //构造分页构造器
            Page<Book> pageinfo = new Page(page, pageSize);
            //9.要注意设置total值不然会有bug
            pageinfo.setTotal(response.getHits().getTotalHits().value);
            pageinfo.setRecords(books);
            return R.success(pageinfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除单个书籍文档
     * @param id
     */
    @Override
    public void deleteDoc(Long id) {
        try {
            DeleteRequest request = new DeleteRequest("books",id.toString());
            client.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("删除书籍文档失败",e);
        }
    }

    /**
     * 新增或修改书籍文档
     * @param id
     */
    @Override
    public void insertOrUpdateById(Long id) {
        Book book = this.getById(id);
        try {
            BookEs bookEs = new BookEs(book);
            //准备request对象
            IndexRequest request = new IndexRequest("books").id(id.toString());
            //准备json文档
            request.source(JSON.toJSONString(bookEs), XContentType.JSON);
            //发送请求
            client.index(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("新增书籍或修改书籍文档失败",e);
        }
    }

    /**
     * 监听书籍点击量的业务
     * @param id
     */
    @Override
    public void UpdateclickDoc(Long id) {
        try {
            // 1.准备获取文档的请求
            GetRequest getRequest = new GetRequest("books",id.toString());
            // 2.发起请求获取结果
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            // 3.解析
            Map<String, Object> sourceAsMap = response.getSourceAsMap();
            // 获取 clicks 字段的值
            int currentClicks = (int) sourceAsMap.get("clicks");
            int newClicks = currentClicks + 1;
            //准备request对象
            UpdateRequest updateRequest = new UpdateRequest("books",id.toString()).doc("clicks",newClicks);
            //发送请求
            client.update(updateRequest,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("增加点击量失败",e);
        }
        //删除缓存
        redisUtil.deleteKeysBypattern(PAGE_SELECT_NONAME);
    }

    /**
     * 定时任务同步数据库
     */
    @Override
    public void syncClicks() {
        try {
            // 1.准备获取文档的请求
            SearchRequest searchRequest = new SearchRequest("books");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.size(10000);
            searchRequest.source(searchSourceBuilder);
            // 2.发起请求获取结果
            SearchResponse response = client.search(
                    searchRequest, RequestOptions.DEFAULT);
            // 3.处理结果,更新数据库
            for (SearchHit hit : response.getHits().getHits()){
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Integer clicks = (Integer) sourceAsMap.get("clicks");
                Long bookId = Long.valueOf(sourceAsMap.get("bookId").toString());
                if (clicks!=null){
                    UpdateWrapper<Book> bookUpdateWrapper = new UpdateWrapper<>();
                    bookUpdateWrapper.eq("book_id",bookId);
                    Book book = new Book();
                    book.setClicks(clicks);
                    this.update(book,bookUpdateWrapper);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * 处理结果
     *
     * @param response
     * @return
     */
    private List<Book> handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 4.2.获取文档数组
        SearchHit[] hits = searchHits.getHits();
        // 4.3.遍历
        List<Book> bookList = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            // 4.4.获取source
            String json = hit.getSourceAsString();
            // 4.5.反序列化
            BookEs bookEs = JSON.parseObject(json, BookEs.class);
            Book book = new Book();
            BeanUtils.copyProperties(bookEs, book);
            // 4.7.放入集合
            bookList.add(book);
        }
        return bookList;
    }
}
