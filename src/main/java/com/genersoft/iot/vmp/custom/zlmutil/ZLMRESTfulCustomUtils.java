package com.genersoft.iot.vmp.custom.zlmutil;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class ZLMRESTfulCustomUtils {

    public String getSnapBase64(MediaServerItem mediaServerItem, String flvUrl, int timeout_sec, int expire_sec) {
        Map<String, Object> param = new HashMap<>();
        param.put("url", flvUrl);
        param.put("timeout_sec", timeout_sec);
        param.put("expire_sec", expire_sec);
        return sendGetForImgBase64(mediaServerItem, "getSnap", param);
    }


    /**
     * 获取流地址当前的截图并返回base64字符串
     *
     * @param mediaServerItem 流媒体服务对象
     * @param api             流地址
     * @param params          参数
     * @return 图片的base64
     */
    public String sendGetForImgBase64(MediaServerItem mediaServerItem, String api, Map<String, Object> params) {
        String url = String.format("http://%s:%s/index/api/%s", mediaServerItem.getIp(), mediaServerItem.getHttpPort(), api);
        log.debug(url);
        HttpUrl parseUrl = HttpUrl.parse(url);
        if (parseUrl == null) {
            return null;
        }
        HttpUrl.Builder httpBuilder = parseUrl.newBuilder();

        httpBuilder.addQueryParameter("secret", mediaServerItem.getSecret());
        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                httpBuilder.addQueryParameter(param.getKey(), param.getValue().toString());
            }
        }
        Request request = new Request.Builder()
                .url(httpBuilder.build())
                .build();
        log.info(request.toString());
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                /*
                获取到的图片流转base64
                 */
                log.info("response body contentType: " + Objects.requireNonNull(response.body()).contentType());
                byte[] buffer = Objects.requireNonNull(response.body()).bytes();
                Base64.Encoder encoder = Base64.getEncoder();
                Objects.requireNonNull(response.body()).close();
                return encoder.encodeToString(buffer);
            } else {
                log.error(String.format("[ %s ]请求失败: %s %s", url, response.code(), response.message()));
            }
        } catch (ConnectException e) {
            log.error(String.format("连接ZLM失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            log.info("请检查media配置并确认ZLM已启动...");
        } catch (IOException e) {
            log.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
        }
        return null;
    }
}
