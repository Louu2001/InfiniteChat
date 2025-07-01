package com.lou.contactservice.data.inviteGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class InviteGroupResponse {

    private List<Long> successIds;

    private List<Long> failedIds;
}
