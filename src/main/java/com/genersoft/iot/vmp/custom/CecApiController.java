package com.genersoft.iot.vmp.custom;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.custom.pojo.StreamInfoImg;
import com.genersoft.iot.vmp.custom.zlmutil.ZLMRESTfulCustomUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@SuppressWarnings("rawtypes")
/**
 * 拉流代理接口
 */
//@Api(tags = "cec-api")
@Controller
@CrossOrigin
@RequestMapping(value = "api/cec")
public class CecApiController {

    private final static Logger logger = LoggerFactory.getLogger(CecApiController.class);

    @Autowired
    private ZLMRESTfulCustomUtils zlmresTfulCustomUtils;
    @Autowired
    private IMediaServerService mediaServerService;
    @Autowired
    private IStreamProxyService streamProxyService;
    @Autowired
    private IMediaService mediaService;


//    @ApiOperation("根据应用名和流id获取播放地址及截图")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "app", value = "应用名", dataTypeClass = String.class),
//            @ApiImplicitParam(name = "stream", value = "流id", dataTypeClass = String.class),
//            @ApiImplicitParam(name = "mediaServerId", value = "媒体服务器id", dataTypeClass = String.class, required = false),
//    })
    @GetMapping(value = "/getStreamInfoWithImgBase64")
    @ResponseBody
    public WVPResult<StreamInfoImg> getStreamInfoWithImgBase64(HttpServletRequest request, @RequestParam String app,
                                                                @RequestParam String stream,
                                                                @RequestParam(required = false) String mediaServerId,
                                                                @RequestParam(required = false) String callId,
                                                                @RequestParam(required = false) Boolean useSourceIpAsStreamIp) {
        WVPResult<StreamInfoImg> result = new WVPResult<>();
        boolean authority = false;
        StreamInfo streamInfo;
        if (useSourceIpAsStreamIp != null && useSourceIpAsStreamIp) {
            String host = request.getHeader("Host");
            String localAddr = host.split(":")[0];
            logger.info("使用{}作为返回流的ip", localAddr);
            streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, localAddr, authority);
        } else {
            streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
        }
        if (Objects.isNull(streamInfo)) {
            //获取流失败，重启拉流后重试一次
            streamProxyService.stop(app, stream);
            boolean start = streamProxyService.start(app, stream);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (useSourceIpAsStreamIp != null && useSourceIpAsStreamIp) {
                String host = request.getHeader("Host");
                String localAddr = host.split(":")[0];
                logger.info("使用{}作为返回流的ip", localAddr);
                streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, localAddr, authority);
            } else {
                streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
            }
            if (Objects.isNull(streamInfo)) {
                result.setCode(-1);
                result.setMsg("fail");
                return result;
            }
        }
        /*
        获取截图
         */
        MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
        String snapBase64 = zlmresTfulCustomUtils.getSnapBase64(mediaInfo, streamInfo.getFmp4().getUrl(), 15, 1);

        StreamInfoImg streamInfoImg = new StreamInfoImg();
        BeanUtils.copyProperties(streamInfo, streamInfoImg);
        streamInfoImg.setImgBase64(snapBase64);
        result.setCode(0);
        result.setMsg("scccess");
        result.setData(streamInfoImg);
        return result;
    }

}
