package com.OLP.author.controller;


import com.OLP.author.Rpc.OssRpc;
import com.OLP.author.service.PublicationService;
import com.OLP.books.rpc.UserRpc;
import com.OLP.common.dto.BookDto;
import com.OLP.common.entity.MqConstants;
import com.OLP.common.entity.R;
import com.OLP.common.exception.DataException;
import com.OLP.common.pojo.Book;
import com.OLP.common.pojo.Publication;
import com.OLP.common.pojo.Recommend;
import com.OLP.common.redis.RedisUtil;
import com.OLP.common.util.LoginUtil;
import com.OLP.common.vo.chapterPreview;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.OLP.books.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.OLP.common.entity.SystemConstants.PAGE_SELECT_NONAME;

/**
 * 书籍与作者关系
 */
@RestController
@RequestMapping("/publish")
@Slf4j
public class PublicationController {
    @Autowired
    private PublicationService publicationService;

    @Resource
    private UserRpc userRpc;

    @Resource
    private OssRpc ossRpc;

    @Autowired
    private BookService bookService;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private BookshelfService bookshelfService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${OnlineBooks.path}")
    private String basePath;


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    /**
     * 查看我上传的书籍
     * @return
     */
    @GetMapping("/mypublishbooks")
    public R<List<Book>> getMypublishBooks(){
        Long Id = LoginUtil.getLoginId();
        LambdaQueryWrapper<Publication> publam = new LambdaQueryWrapper<>();
        publam.eq(Publication::getAuthorId,Id);
        List<Publication> list = publicationService.list(publam);
        List<Long> bookIds = list.stream()
                .map(Publication::getBookId)
                .collect(Collectors.toList());
        //根据得到的bookId查询书籍信息
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        booklam.in(Book::getBookId,bookIds);
        List<Book> booklist = null;
        try {
            booklist = bookService.list(booklam);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataException("你还没有发布书籍!");
        }
        return R.success(booklist);
    }

    /**
     * 作者上传书籍
     * @return
     */
    @PostMapping("/addBook")
    public R<String> addBook(BookDto bookDto){
        log.info("{}",bookDto);
        MultipartFile coverImg = bookDto.getCoverImg();
        //初始文件名
        String originalFilename = coverImg.getOriginalFilename();

        //pojo 对象部分
        //book表
        Book book = new Book();
        book.setBookName(bookDto.getBookName());
        Long id = LoginUtil.getLoginId();
        String authorName = userRpc.getById(id).getUsername();
        book.setAuthor(authorName);
        book.setPublicationDate(LocalDate.now());
        book.setDescription(bookDto.getDescription());
        book.setCategory(bookDto.getCategory());
        //重新设置书籍封面路径
        String pictureUrl = bookDto.getBucket()  + "/" + bookDto.getObjectName()  + "/" + originalFilename;
        book.setCoverUrl(pictureUrl);
        bookService.save(book);
        //调用oss
        ossRpc.OssuploadAvatar(bookDto.getCoverImg(),bookDto.getBucket(),bookDto.getObjectName());
        //publication表
        Publication pub = new Publication();
        pub.setAuthorId(id);
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        //书名唯一
        booklam.eq(Book::getBookName,bookDto.getBookName());
        Book bookone = bookService.getOne(booklam);
        pub.setBookId(bookone.getBookId());
        pub.setStatus("3");
        publicationService.save(pub);
        //status在数据库默认为0 ->审核

        return R.success("上传书籍成功!");
    }

    @PostMapping("/addChapter")
    public R<String> addChapter(@RequestBody Publication publication){
        publication.setStatus("0");
        if (publication.getChapter()==null|| !StringUtils.hasLength(publication.getChContent())||!StringUtils.hasLength(publication.getChTitle())){
            return R.error("章节序号、标题、内容不能为空!");
        }
        publicationService.save(publication);
        return R.success("上传章节成功！");
    }

    /**
     * 上传作者资质
     * @param proofUrl
     * @return
     */
    @PostMapping("/UploadProof")
    public R<String> UploadProof(@RequestParam("proofUrl")MultipartFile proofUrl){
        //初始文件名
        String originalFilename = proofUrl.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //创建一个目录对象
        File dir = new File(basePath);
        //判断当前目录是否存在
        if (!dir.exists()){
            dir.mkdirs();//目录不存在就创建目录
        }
        //使用UUID生成新文件名
        String fileName = UUID.randomUUID() + suffix;
        try{
            //将临时文件转存到指定位置
            proofUrl.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //装填url到user
        userRpc.setproof(basePath + fileName);
        return R.success("上传资质成功！");
    }

    @PostMapping("/updateBooks")
    public R<String> updateBooks(@RequestBody Book book){
        if (book.getBookId()==null){
            throw new DataException("修改书籍id为空");
        }
        if ("1".equals(book.getStatus())){
            //第一次删
            redisUtil.deleteKeysBypattern(PAGE_SELECT_NONAME);
            bookService.updateById(book);
            //延迟双删
            long delayMillis = 5000; // 延迟5秒
            scheduleDelayedDelete(PAGE_SELECT_NONAME,delayMillis);
            //发送消息同步数据(增加/修改)
            rabbitTemplate.convertAndSend(MqConstants.BOOKS_EXCHANGE,MqConstants.BOOKS_INSERTORUPDATE_KEY,book.getBookId());
        }else {
            bookService.updateById(book);
        }
        return R.success("修改书籍信息成功");
    }

    @DeleteMapping("/DeleteBook")
    public R<String> DeleteBook(Long bookId){
        if (bookId==null){
            throw new DataException("删除的书籍不存在");
        }
        //先删除这本书的所有章节
        LambdaQueryWrapper<Publication> publam = new LambdaQueryWrapper<>();
        publam.eq(Publication::getBookId,bookId);
        publicationService.remove(publam);
        //再删除推荐表里的数据
        LambdaQueryWrapper<Recommend> reclam = new LambdaQueryWrapper<>();
        reclam.eq(Recommend::getBookId,bookId);
        recommendService.remove(reclam);
        //最后删除书籍
        //先判断是不是已发布状态的书籍
        if ("1".equals(bookService.getById(bookId).getStatus())){
            //第一次删
            redisUtil.deleteKeysBypattern(PAGE_SELECT_NONAME);
            bookService.removeById(bookId);
            //延迟双删
            long delayMillis = 5000; // 延迟5秒
            scheduleDelayedDelete(PAGE_SELECT_NONAME,delayMillis);
            //发送消息同步数据(删除)
            rabbitTemplate.convertAndSend(MqConstants.BOOKS_EXCHANGE,MqConstants.BOOKS_DELETE_KEY,bookId);
        }
        else{bookService.removeById(bookId);}
        return R.success("删除书籍成功");
    }



    private void scheduleDelayedDelete(String pattern, long delayMillis) {
        scheduler.schedule(() -> {
            System.out.println("延迟删除缓存: {}" + pattern);
            redisUtil.deleteKeysBypattern(pattern);
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    @DeleteMapping("/deleteChapter")
    public R<String> deleteChapter(Long bookId,Integer chapter){
        if (bookId==null||chapter==null){
            throw new DataException("删除的书籍或章节不存在");
        }
        LambdaQueryWrapper<Publication> publam = new LambdaQueryWrapper<>();
        publam.eq(Publication::getBookId,bookId).eq(Publication::getChapter,chapter);
        publicationService.remove(publam);
        return R.success("删除章节成功");
    }

    /**
     * 章节试读
     * @param bookId
     * @return
     */
    @GetMapping("/ChapterPreview")
    public R<List<chapterPreview>> ChapterPreview(Long bookId){
        LambdaQueryWrapper<Publication> pubLam = new LambdaQueryWrapper<>();
        pubLam.eq(Publication::getBookId,bookId);
        List<Publication> list = publicationService.list(pubLam);
        if (list==null||list.size()==0){
            throw new DataException("该书籍没有试读内容");
        }
        List<chapterPreview> chapterList = new ArrayList<>();
        for (Publication item : list) {
            chapterPreview chapterPreview = new chapterPreview();
            chapterPreview.setChapter(item.getChapter());
            chapterPreview.setChTitle(item.getChTitle());
            chapterPreview.setChContent(item.getChContent());
            chapterList.add(chapterPreview);
        }
        return R.success(chapterList);
    }

    /**
     * 阅读量统计
     * @return
     */
    @GetMapping("/readCount")
    public R<Map<String,Integer>> readCount(String username){
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        booklam.eq(Book::getAuthor,username).eq(Book::getStatus,"1");
        List<Book> booklist = bookService.list(booklam);
        if (booklist==null || booklist.size()==0){
            throw new DataException("还没有发布书籍");
        }
        Map<String,Integer> countMap = new HashMap<>();
        for (Book book : booklist) {
            countMap.put(book.getBookName(),book.getClicks());
        }

        return R.success(countMap);
    }
    /**
     * 阅读时长统计
     * @return
     */
    @GetMapping("/timeCount")
    public R<Map<String,Integer>> timeCount(String username){
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        booklam.eq(Book::getAuthor,username).eq(Book::getStatus,"1");
        List<Book> booklist = bookService.list(booklam);
        if (booklist==null || booklist.size()==0){
            throw new DataException("还没有发布书籍");
        }
        Map<String,Integer> countMap = new HashMap<>();
        Map<Long,Integer> preMap = new HashMap<>();
        Map<Long,Integer> middleMap = new HashMap<>();
        for (Book book : booklist) {
            LambdaQueryWrapper<Recommend> recLam = new LambdaQueryWrapper<>();
            recLam.eq(Recommend::getBookId,book.getBookId());
            //得到以BookId为键，时间总和为值的map
            preMap = recommendService.list(recLam)
                    .stream()
                    .collect(Collectors.groupingBy(Recommend::getBookId,Collectors.summingInt(Recommend::getTotalTime)));

            middleMap.put(book.getBookId(), preMap.get(book.getBookId()));

            //清空
            recLam.clear();
        }

        //将书的id换成书名
        for (Map.Entry<Long, Integer> entry : middleMap.entrySet()) {
            String bookName = bookService.getById(entry.getKey()).getBookName();
            //如果value为null前端会自动去掉这条数据
            if (entry.getValue()==null) {
                countMap.put(bookName, 0);
            }
            else countMap.put(bookName, entry.getValue());
        }
        return R.success(countMap);
    }

    /**
     * 今年各月份该作者所有书收藏量幅度对比
     * @return
     */
    @GetMapping("/YearCount")
    public R<Map<String,Long>> TheYearRCount(String username){
        Map<String,Long> countMap = bookshelfService.collectMonth(username);
        return R.success(countMap);
    }

}
