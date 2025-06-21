package com.lou.momentservice.controller;

import com.lou.momentservice.common.Result;
import com.lou.momentservice.data.createLike.CreateLikeRequest;
import com.lou.momentservice.data.createLike.CreateLikeResponse;
import com.lou.momentservice.data.createMoment.CreateMomentRequest;
import com.lou.momentservice.data.createMoment.CreateMomentResponse;
import com.lou.momentservice.service.MomentLikeService;
import com.lou.momentservice.service.MomentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @ClassName MomentController
 * @Description TODO
 * @Author Lou
 * @Date 2025/6/21 19:54
 */

@Slf4j
@RestController
@RequestMapping("/api/v1/moment")
@RequiredArgsConstructor
public class MomentController {
    @Autowired
    private MomentService momentService;

    @Autowired
    private MomentLikeService momentLikeService;

    @PostMapping("")
    public Result<CreateMomentResponse> createMoment(@Valid @RequestBody CreateMomentRequest request) throws Exception {
        CreateMomentResponse response = momentService.createMoment(request);

        return Result.OK(response);
    }

    @PostMapping("/like/{momentId}")
    public Result<CreateLikeResponse> likeMoment(@PathVariable Long momentId, @Valid @RequestBody CreateLikeRequest request) throws Exception {
        CreateLikeResponse response = momentLikeService.likeMomentResponse(momentId,request);

        return Result.OK(response);
    }

}
