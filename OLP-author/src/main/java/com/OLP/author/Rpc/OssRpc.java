package com.OLP.author.Rpc;
import com.OLP.api.OssFeignService;
import com.OLP.common.entity.R;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@Component
public class OssRpc {

    @Resource
    private OssFeignService ossFeignService;

    /**
     * 上传头像(加入了oss)
     *
     * @return
     */
    public R<String> OssuploadAvatar(@RequestPart("uploadFile") MultipartFile uploadFile,
                                     @RequestParam("bucket") String bucket,
                                     @RequestParam("objectName") String objectName){
                return R.success(ossFeignService.upload(uploadFile,bucket,objectName));
    }
}
