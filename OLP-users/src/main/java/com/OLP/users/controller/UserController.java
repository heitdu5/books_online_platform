package com.OLP.users.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.OLP.common.entity.R;
import com.OLP.common.entity.SystemConstants;
import com.OLP.common.exception.DataException;
import com.OLP.common.exception.ServiceException;
import com.OLP.common.dto.UserDto;
import com.OLP.common.pojo.User;
import com.OLP.users.properties.JwtProperties;
import com.OLP.common.redis.RedisUtil;
import com.OLP.users.mapper.UserMapper;
import com.OLP.users.rpc.OssRpc;
import com.OLP.users.service.UserService;
import com.OLP.users.utils.JwtUtil;
import com.OLP.common.util.LoginUtil;
import com.OLP.users.utils.SMSUtils;
import com.OLP.users.utils.ValidateCodeUtils;
import com.OLP.users.vo.UserLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.base.Preconditions;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 用户管理
 */
@Slf4j
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserService userService;
    @Value("${OnlineBooks.path}")
    private String basePath;
    @Autowired
    private UserMapper userMapper;

    @Resource
    private OssRpc ossRpc;
    @Resource
    private RedisUtil redisUtil;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/register")
    public R<String> register(@RequestBody Map<String, String> formData) {
        log.info(" {}  {}  {}", formData);
        User user = new User();
        String username = formData.get("account");
        //判断用户名是否被注册
        List<User> list = userService.list();
        List<String> usernames = list.stream().map(User::getUsername).collect(Collectors.toList());
        if (usernames.contains(username)) {
            return R.error("抱歉，该用户名已被注册");
        }
        String phonenumber = formData.get("phonenumber");
        //判断手机号是否被注册
        List<String> phonenumbers = list.stream().map(User::getPhoneNumber).collect(Collectors.toList());
        if (phonenumbers.contains(phonenumber)) {
            return R.error("抱歉，该手机号已被注册");
        }
        String password = formData.get("password");
        user.setUsername(username);
        user.setPassword(password);
        user.setPhoneNumber(phonenumber);
        user.setStatus(true);
        user.setRights("user");
        userService.save(user);
        return R.success("注册成功");
    }

    /**
     * 登录
     *
     * @return
     */
    @PostMapping("/login")
    public R<SaTokenInfo> doLogin(@RequestBody Map<String, String> formData, HttpSession session, HttpServletRequest request) {
        log.info("登录{}", formData);
        //验证user信息
        User user = userService.login(formData);
        String checkCode = formData.get("checkCode");
        String code = (String) redisTemplate.opsForValue().get(SystemConstants.LOGIN_VALIDATECODE);
        try {
            Preconditions.checkArgument(!org.apache.commons.lang3.StringUtils.isBlank(checkCode), "验证码不能为空!");
            if (!checkCode.equals(code)) {
                throw new DataException("验证码错误!");
            }
            //生成JWT令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", user.getUserId());
//            String jwt = JwtUtil.createJWT(jwtProperties.getAdminSecretkey(), jwtProperties.getAdminTtl(), claims);
            StpUtil.login(user.getUserId());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            return R.success(tokenInfo);
        } catch (Exception e) {
            log.error("UserController.doLogin.error:{}", e.getMessage(), e);
            return R.error("用户登录失败");
        }
    }


    // 查询登录状态，浏览器访问： http://localhost:8081/user/isLogin
    @RequestMapping("isLogin")
    public String isLogin() {
        return "当前会话是否登录：" + StpUtil.isLogin();
    }

    /**
     * 微服务拆分之前的登录接口
     */
//    @PostMapping("/login")
//    public R<UserLoginVo> login(@RequestBody Map<String,String> formData, HttpSession session,HttpServletRequest request){
//        log.info("登录{}",formData);
//        User user = userService.login(formData);
//        //用户信息没问题再确定验证码
//        String checkCode = formData.get("checkCode");
////        String code = (String) request.getSession().getAttribute("checkcode");
//        String code = (String) redisTemplate.opsForValue().get(SystemConstants.LOGIN_VALIDATECODE);
//        if (!checkCode.equals(code)){
//            log.info("验证码错误!");
//            throw new DataException("验证码错误!");
//        }
//        //生成JWT令牌
//        Map<String,Object> claims= new HashMap<>();
//        claims.put("id",user.getUserId());
//        String jwt = JwtUtil.createJWT(jwtProperties.getAdminSecretkey(), jwtProperties.getAdminTtl(), claims);
////        微服务拆分，现在不用Jwt了
//        //将数据存储到session对象，这里用来给websocket逻辑
//        session.setAttribute("username",user.getUsername());
//
//        //封装结果并返回
//        UserLoginVo userLoginVo = new UserLoginVo(user.getUserId(),
//                user.getUsername(),jwt,user.getAvatarurl(),
//                user.getIsAuthor(),user.getStatus(),
//                user.getRights());
//        return R.success(userLoginVo);
//    }

    /**
     * 返回登录用户信息
     *
     * @return
     */
    @GetMapping("/info")
    public R<UserLoginVo> userinfo(HttpServletRequest request) {
        String jwt = request.getHeader("loginId");
        Long id = LoginUtil.getLoginId();
        LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
        userlam.eq(User::getUserId, id);
        User user = userService.getOne(userlam);
        UserLoginVo userLoginVo = new UserLoginVo(user.getUserId(),
                user.getUsername(), jwt, user.getAvatarurl(), user.getIsAuthor(),
                user.getStatus(), user.getRights());
        return R.success(userLoginVo);
    }

    /**
     * 得到用户登录信息
     *
     * @param request
     * @return
     */
    @GetMapping("/getuserInfo")
    public R<User> getuserInfo(HttpServletRequest request) {
        Long id = LoginUtil.getLoginId();
        LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
        userlam.eq(User::getUserId, id);
        User user = userService.getOne(userlam);
        return R.success(user);
    }

    /**
     * 修改个人信息
     *
     * @param
     * @return
     */
    @PutMapping("/infoUpdate")
    public R<String> infoUpdate(@RequestBody User user) {
        Long id = LoginUtil.getLoginId();
        user.setUserId(id);
        userService.updateById(user);
        return R.success("修改个人信息成功");
    }

//    /**
//     * 上传头像
//     *
//     * @param imgUrl
//     * @return
//     */
//    @PostMapping("/uploadAvatar")
//    public R<String> uploadAvatar(@RequestParam("imgUrl") MultipartFile imgUrl) {
//        //初始文件名
//        String originalFilename = imgUrl.getOriginalFilename();
//        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
//
//        //创建一个目录对象
//        File dir = new File(basePath);
//        //判断当前目录是否存在
//        if (!dir.exists()) {
//            dir.mkdirs();//目录不存在就创建目录
//        }
//        //使用UUID生成新文件名
//        String fileName = UUID.randomUUID() + suffix;
//        try {
//            //将临时文件转存到指定位置
//            imgUrl.transferTo(new File(basePath + fileName));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        //装填url到user
//        userService.setAvatar(basePath + fileName);
//        return R.success("上传头像成功！");
//    }

    /**
     * 上传头像(加入了oss)
     *
     * @return
     */
    @PostMapping("/uploadAvatar")
    public R<String> OssuploadAvatar(@RequestPart("uploadFile") MultipartFile uploadFile,
                                     @RequestParam("bucket") String bucket,
                                     @RequestParam("objectName") String objectName) {
        Long id = LoginUtil.getLoginId();
        User user = userService.getById(id);
        //初始文件名
        String originalFilename = uploadFile.getOriginalFilename();
        String name =   bucket + "/" + objectName+ "/" + originalFilename;
        //装填url到user
        user.setAvatarurl(name);
        userService.updateById(user);
        return ossRpc.OssuploadAvatar(uploadFile,bucket,objectName);
    }


    /**
     * 展示头像
     *
     * @param name
     * @param response
     */
    @GetMapping("/getMyavatar")
    public void getMyAvatar(String name, HttpServletResponse response) {
        try {
            response.addHeader("Access-Contro1-Allow-Origin", "*");
            //输入流，通过输入流读取内容
            FileInputStream fileInputStream = new FileInputStream(new File(name));
            response.setContentType("image/jpeg");

            //输出流，通过输出流将文件写回浏览器，在浏览器里展示图片
            ServletOutputStream outputStream = response.getOutputStream();

            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = fileInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }
            //关闭资源
            outputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/auditUsers")
    public R<List<User>> auditUsers(@RequestBody User user) {
        LambdaQueryWrapper<User> userLam = new LambdaQueryWrapper<>();
        userLam.eq(User::getStatus, true).eq(User::getRights, "user").orderByDesc(User::getCreateTime);
        //没有筛选条件
        if (!StringUtils.hasLength(user.getUsername()) && user.getAuthorTime() == null) {
            List<User> list = userService.list(userLam);
            return R.success(list);
        }
        if (StringUtils.hasLength(user.getUsername())) {
            userLam.like(User::getUsername, user.getUsername());
        }
        if (user.getAuthorTime() != null) {
            userLam.eq(User::getAuthorTime, user.getAuthorTime());
        }
        List<User> list = userService.list(userLam);
        //判空
        if (list == null || list.size() == 0) {
            throw new DataException("没有查询到正常的用户");
        }
        return R.success(list);
    }

    /**
     * 注册成为作者或封禁按钮
     *
     * @param userDto
     * @return
     */
    @PutMapping("/registerAuthor")
    public R<String> registerAuthor(@RequestBody UserDto userDto) {
        if (userDto.getBanOrRe()) {
            User user = userService.getById(userDto.getUserId());
            user.setIsAuthor(true);
            user.setAuthorTime(LocalDate.now());
            userService.updateById(user);
        } else {
            User user = userService.getById(userDto.getUserId());
            user.setStatus(false);
            userService.updateById(user);
        }
        return R.success("注册作者或封禁用户成功");
    }

    @GetMapping("/banUsers")
    public R<String> banUsers(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new DataException("待封禁的用户不存在");
        }
        user.setStatus(false);
        userService.updateById(user);
        return R.success("注册作者或封禁用户成功");
    }

    /**
     * 点击每一行用户时显示的数据
     *
     * @param userId
     * @return
     */
    @GetMapping("/userDetail")
    public R<User> userDetail(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new DataException("该用户不存在!");
        }
        return R.success(user);
    }

    @PostMapping("/userManageUpdate")
    public R<String> userManageUpdate(@RequestBody User user) {
        if (user.getUserId() != null) {
            userService.updateById(user);
        }
        return R.success("修改用户信息成功!");
    }

    /**
     * 发送手机短信验证码
     *
     * @param phoneNumber
     * @return
     */
    @GetMapping("/findpassword")
    public R<String> sendMsg(String phoneNumber, HttpSession session) {
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(phoneNumber)) {
            //检查手机格式
            String regex = "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(phoneNumber);
            if (phoneNumber.length() != 11 || !m.matches()) {
                return R.error("手机长度不够11位或格式不对");
            }
            //生成六位随机验证码
            String code = ValidateCodeUtils.generateValidateCode(6).toString();
            log.info("code = {}", code);
            //设置Zset评分，实现用户手机发送验证码次数限制
            LocalDate today = LocalDate.now();                                  //如果键不存在会自动创建
            redisTemplate.boundZSetOps(SystemConstants.USER_SEND_FREQUENCY + phoneNumber).incrementScore(phoneNumber + today, 1D);

            //获取分数
            Double score = redisTemplate.opsForZSet().score(SystemConstants.USER_SEND_FREQUENCY + phoneNumber, phoneNumber + today);
            if (score > 3.0) {
                throw new ServiceException("发送次数超过上限！");
            }
            //设置过期时间
            redisTemplate.expire(SystemConstants.USER_SEND_FREQUENCY + phoneNumber, 24L, TimeUnit.HOURS);

            //调用阿里云提供的短信服务API完成发送短信
            SMSUtils.sendMessage("黑T毒物","SMS_464066219",phoneNumber,code);

            //将生成的验证码保存到session
            session.setAttribute(phoneNumber, code);
            //设置时间为120s
            session.setMaxInactiveInterval(2 * 60);
            return R.success("短信发送成功");
        }
        return R.error("短信发送失败");
    }


    /**
     * 点击确认找回
     *
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/confirmFind")
    public R<String> confirmFind(@RequestBody Map<String, String> map, HttpSession session) {

        //获取手机号
        String phone = map.get("phone");
        //获取验证码
        String code = map.get("code");

        //从session中获取保存的验证码
        Object codeInSession = session.getAttribute(phone);
        //比对得到的验证码
        if (codeInSession != null && codeInSession.equals(code)) {
            LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
            userlam.eq(User::getPhoneNumber, phone);
            return R.success(userService.getOne(userlam).getPassword());
        }
        return R.error("验证码比对失败");
    }

    /**
     * ws服务调用
     * @param username
     * @return
     */
    @GetMapping("/getFreindsList")
    public R<List<User>> FreindsList(@RequestParam(name = "username",required = false)  String username){
        LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
        List<User> userList = new ArrayList<>();

            userlam.eq(User::getStatus,true);

        //有搜索名字
        if (StringUtils.hasLength(username)) {
            userlam.eq(User::getStatus, true).like(User::getUsername, username);
        }
        userList = userService.list(userlam);

        if (userList==null|| userList.size()==0){
            throw new DataException("没有用户存在");
        }
        return R.success(userList);
    }

    /**
     * 微服务调用提供方法
     * @param id
     * @return
     */
    @GetMapping("/getById")
    public User getById(@RequestParam("id") Long id){
        return userService.getById(id);
    }


    /**
     * 微服务调用提供方法
     * @return
     */
    @GetMapping("/getListByCondition")
    public List<User> getByCondition(@RequestParam("reactUsers")  List<String> reactUsers){
        LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
        userlam.in(User::getUsername,reactUsers);
        return userService.list(userlam);
    }

    /**
     * 微服务调用提供方法
     * @return
     */
    @GetMapping("/getOneByCondition")
    public User getOneByCondition(@RequestParam("name")  String name){
        LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
        userlam.eq(User::getUsername,name);
        return userService.getOne(userlam);
    }


    /**
     * 微服务调用提供方法
     * @return
     */
    @GetMapping("setproof")
    public void setproof(@RequestParam("s") String s){
        Long id = LoginUtil.getLoginId();
        //处理路径格式
        String pictureUrl = s.replace("\\","/").replaceFirst(":",":/");
        userMapper.setproof(pictureUrl,id);

    }


}
