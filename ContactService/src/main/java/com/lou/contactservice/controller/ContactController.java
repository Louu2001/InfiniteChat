package com.lou.contactservice.controller;

import com.lou.contactservice.common.Result;
import com.lou.contactservice.data.AddFriend.AddFriendRequest;
import com.lou.contactservice.data.AddFriend.AddFriendResponse;
import com.lou.contactservice.data.ApplyList.ApplyListRequest;
import com.lou.contactservice.data.ApplyList.ApplyListResponse;
import com.lou.contactservice.data.BlockFriend.BlockFriendRequest;
import com.lou.contactservice.data.BlockFriend.BlockFriendResponse;
import com.lou.contactservice.data.DeleteFriend.DeleteFriendRequest;
import com.lou.contactservice.data.DeleteFriend.DeleteFriendResponse;
import com.lou.contactservice.data.ModifyApply.ModifyApplyRequest;
import com.lou.contactservice.data.ModifyApply.ModifyApplyResponse;
import com.lou.contactservice.data.SearchUser.SearchUserRequest;
import com.lou.contactservice.data.SearchUser.SearchUserResponse;
import com.lou.contactservice.data.UnreadApply.UnreadApplyRequest;
import com.lou.contactservice.data.UnreadApply.UnreadApplyResponse;
import com.lou.contactservice.service.ApplyFriendService;
import com.lou.contactservice.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.PublicKey;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private ApplyFriendService applyFriendService;

//    @GetMapping("/user")
//    public Result<UserResponse> getUser() {
//        UserResponse response = new UserResponse();
//        response.setAvatar("www.baidu.com");
//
//        return Result.ok(response);
//    }

    @GetMapping("/{userUUid}/user")
    public Result<SearchUserResponse> searchUser(@Valid @ModelAttribute SearchUserRequest request) {
        SearchUserResponse response = friendService.getUserDetails(request);

        return Result.OK(response);
    }

    @PostMapping("/{userUuid}/friend/{receiveUserUuid}")
    public Result<?> addFriend(@NotNull(message = "发起人不能为空") @PathVariable("userUuid") String userUuid,
                               @NotNull(message = "接收者不能为空") @PathVariable("receiveUserUuid") String receiveUserUuid,
                               @RequestBody AddFriendRequest request) throws Exception {
        applyFriendService.addFriend(userUuid, receiveUserUuid, request);
        return Result.OK(1);
    }

    @GetMapping("/{userUuid}/applyCount")
    public Result<UnreadApplyResponse> getUnreadApplyCount(@Valid @ModelAttribute UnreadApplyRequest request) {
        UnreadApplyResponse response = applyFriendService.getUnreadApply(request);

        return Result.OK(response);
    }


    @GetMapping("/{userUuid}/apply")
    public Result<ApplyListResponse> getApplyList(@Valid @ModelAttribute ApplyListRequest request) {
        ApplyListResponse response = applyFriendService.getApplyList(request);
        return Result.OK(response);
    }

    @PostMapping("{userUuid}/application/{status}")
    public Result<ModifyApplyResponse> modifyFriendApplicationStatus(@Valid @ModelAttribute ModifyApplyRequest request) throws Exception {
        ModifyApplyResponse response = applyFriendService.modifyApply(request);

        return Result.OK(response);
    }


    @DeleteMapping("/{userUuid}/friend/{receiveUserUuid}")
    public Result<DeleteFriendResponse> deleteFriend(@Valid @ModelAttribute DeleteFriendRequest request) {
        DeleteFriendResponse response = friendService.deleteFriend(request);

        return Result.OK(response);
    }

    @PostMapping("/{userUuid}/block/{receiveUserUuid}")
    public Result<BlockFriendResponse> blockFriend(@Valid @ModelAttribute BlockFriendRequest request) throws Exception{
        BlockFriendResponse response = friendService.blockFriend(request);

        return Result.OK(response);
    }

//    @GetMapping("")


}
