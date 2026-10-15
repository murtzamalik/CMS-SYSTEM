package com.cms.app.controller;

import com.cms.app.config.ResponseCode;
import com.cms.app.request.AuthRequest;
import com.cms.app.response.LoginResponse;
import com.cms.app.response.ResponseWrapper;
import com.cms.app.service.UserAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserAuthService userAuthService;

    public UserController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/login")
    public @ResponseBody ResponseWrapper<LoginResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        ResponseWrapper<LoginResponse> response = new ResponseWrapper<>();
        return userAuthService.login(
                        authRequest.getUsername(),
                        authRequest.getPassword(),
                        authRequest.getRelationshipNum())
                .map(body -> {
                    response.setResponseCode(ResponseCode.SUCCESS);
                    response.setResponseMessage(ResponseCode.getMessage(ResponseCode.SUCCESS));
                    response.setResponseBody(body);
                    return response;
                })
                .orElseGet(() -> {
                    response.setResponseCode(ResponseCode.UNAUTHORIZED);
                    response.setResponseMessage(ResponseCode.getMessage(ResponseCode.UNAUTHORIZED));
                    response.setResponseBody(null);
                    return response;
                });
    }
}
