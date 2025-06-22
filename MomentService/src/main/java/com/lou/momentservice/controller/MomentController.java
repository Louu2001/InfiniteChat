package com.lou.momentservice.controller;

import com.lou.momentservice.common.Result;
import com.lou.momentservice.data.deleteLike.DeleteLikeRequest;
import com.lou.momentservice.data.deleteLike.DeleteLikeResponse;
import com.lou.momentservice.data.createComment.CreateCommentRequest;
import com.lou.momentservice.data.createComment.CreateCommentResponse;
import com.lou.momentservice.data.createComment.MomentCommentDTO;
import com.lou.momentservice.data.createLike.CreateLikeRequest;
import com.lou.momentservice.data.createLike.CreateLikeResponse;
import com.lou.momentservice.data.createMoment.CreateMomentRequest;
import com.lou.momentservice.data.createMoment.CreateMomentResponse;
import com.lou.momentservice.data.deleteComment.DeleteCommentRequest;
import com.lou.momentservice.data.deleteComment.DeleteCommentResponse;
import com.lou.momentservice.data.deleteMoment.DeleteMomentRequest;
import com.lou.momentservice.data.deleteMoment.DeleteMomentResponse;
import com.lou.momentservice.data.getMomentList.GetMomentListRequest;
import com.lou.momentservice.data.getMomentList.GetMomentListResponse;
import com.lou.momentservice.service.MomentCommentService;
import com.lou.momentservice.service.MomentLikeService;
import com.lou.momentservice.service.MomentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

    @Autowired
    private MomentCommentService momentCommentService;

    @PostMapping("")
    public Result<CreateMomentResponse> createMoment(@Valid @RequestBody CreateMomentRequest request) throws Exception {
        CreateMomentResponse response = momentService.createMoment(request);

        return Result.OK(response);
    }

    @DeleteMapping("{momentId}")
    public Result<DeleteMomentResponse> deleteMoment(@Valid @ModelAttribute DeleteMomentRequest request) {
        DeleteMomentResponse response = momentService.deleteMoment(request);

        return Result.OK(response);
    }

    @PostMapping("/like/{momentId}")
    public Result<CreateLikeResponse> likeMoment(@PathVariable Long momentId, @Valid @RequestBody CreateLikeRequest request) throws Exception {
        CreateLikeResponse response = momentLikeService.likeMoment(momentId, request);

        return Result.OK(response);
    }

    @DeleteMapping("/like/{momentId}")
    public Result<DeleteLikeResponse> deleteLikeMoment(@Valid @ModelAttribute DeleteLikeRequest request) {
        DeleteLikeResponse response = momentLikeService.deleteLikeMoment(request);

        return Result.OK(response);
    }

    @PostMapping("/comment/{momentId}")
    public Result<CreateCommentResponse> createComment(@NotNull(message = "朋友圈 ID 不能为空")
                                                       @PathVariable("momentId") Long momentId,
                                                       @Valid @RequestBody MomentCommentDTO momentCommentDTO) throws Exception {
        CreateCommentRequest createCommentRequest = new CreateCommentRequest()
                .setMomentId(momentId)
                .setMomentCommentDTO(momentCommentDTO);

        CreateCommentResponse response = momentCommentService.createComment(createCommentRequest);
        return Result.OK(response);
    }

    @DeleteMapping("/comment/{momentId}")
    public Result<DeleteCommentResponse> deleteComment(@Valid @ModelAttribute DeleteCommentRequest request) {
        DeleteCommentResponse response = momentCommentService.deleteComment(request);
        return Result.OK(response);
    }

    @GetMapping("/list/{userId}")
    public Result<GetMomentListResponse> getMomentList(@Valid @ModelAttribute GetMomentListRequest request) {
        GetMomentListResponse response = momentService.getMomentList(request);

        return Result.OK(response);
    }


}
