package com.agora.chat.token;

import com.agora.chat.token.io.agora.chat.ChatTokenBuilder2;
import com.agora.chat.token.io.agora.media.AccessToken2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin

public class AgoraChatTokenController {

    @Value("${appid}")
    private String appid;

    @Value("${appcert}")
    private String appcert;

    @Value("${expire.second}")
    private int expire;

    @Value("${appkey}")
    private String appkey;

    @Value("${domain}")
    private String domain;

    private final RestTemplate restTemplate = new RestTemplate();

    // Gets a token with app privileges.
    @GetMapping("/chat/app/token")
    public String getAppToken() {
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(appcert)) {
            return "appid or appcert is not empty";
        }

        // Generates a token with app privileges.
        AccessToken2 accessToken = new AccessToken2(appid, appcert, expire);
        AccessToken2.Service serviceChat = new AccessToken2.ServiceChat();
        serviceChat.addPrivilegeChat(AccessToken2.PrivilegeChat.PRIVILEGE_CHAT_APP, expire);
        accessToken.addService(serviceChat);

        try {
            return accessToken.build();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // Gets a token with user privileges.
    @GetMapping("/chat/user/{chatUserName}/token")
    public String getChatUserToken(@PathVariable String chatUserName) {
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(appcert)) {
            return "appid or appcert is not empty";
        }
        if (!StringUtils.hasText(appkey) || !StringUtils.hasText(domain)) {
            return "appkey or domain is not empty";
        }
        if (!appkey.contains("#")) {
            return "appkey is illegal";
        }
        if (!StringUtils.hasText(chatUserName)) {
            return "chatUserName is not empty";
        }
        ChatTokenBuilder2 builder = new ChatTokenBuilder2();
        String chatUserUuid = getChatUserUuid(chatUserName);
        if (chatUserUuid == null) {
            chatUserUuid = registerChatUser(chatUserName);
        }

        // Generates a token with user privileges.
        AccessToken2 accessToken = new AccessToken2(appid, appcert, expire);
        AccessToken2.Service serviceChat = new AccessToken2.ServiceChat(chatUserUuid);
        serviceChat.addPrivilegeChat(AccessToken2.PrivilegeChat.PRIVILEGE_CHAT_USER, expire);
        accessToken.addService(serviceChat);

        try {
            return accessToken.build();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // Gets the UUID of the user.
    private String getChatUserUuid(String chatUserName) {
        String orgName = appkey.split("#")[0];
        String appName = appkey.split("#")[1];
        String url = "http://" + domain + "/" + orgName + "/" + appName + "/users/" + chatUserName;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(getAppToken());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(null, headers);
        ResponseEntity<Map> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        } catch (Exception e) {
            System.out.println("get chat user error : " + e.getMessage());
        }
        if (responseEntity != null) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseEntity.getBody().get("entities");
            return (String) results.get(0).get("uuid");
        }
        return null;
    }

    // Creates a user with the password "123", and gets UUID.
    private String registerChatUser(String chatUserName) {
        String orgName = appkey.split("#")[0];
        String appName = appkey.split("#")[1];
        String url = "http://" + domain + "/" + orgName + "/" + appName + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(getAppToken());
        Map<String, String> body = new HashMap<>();
        body.put("username", chatUserName);
        body.put("password", "123");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            throw new RestClientException("register chat user error : " + e.getMessage());
        }
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("entities");
        return (String) results.get(0).get("uuid");
    }

}
