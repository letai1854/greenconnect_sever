package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.BannerGroupCreateRequest;
import com.greenconnect.greenconnect_api.dtos.request.BannerGroupUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.BannerGroupResponse;

public interface BannerGroupService {
    /**
     * Tạo một BannerGroup mới cùng với danh sách các Banner con của nó
     * trong một giao dịch duy nhất.
     *
     * @param request DTO chứa thông tin của nhóm và các banner.
     * @return DTO của nhóm và các banner đã được tạo.
     */
    BannerGroupResponse createGroupWithBanners(BannerGroupCreateRequest request);

    /**
     * Cập nhật một BannerGroup cùng danh sách Banner kèm theo:
     * - Nếu banner trong request có bannerId tồn tại -> update
     * - Nếu banner trong request không có bannerId -> tạo mới
     * - Nếu database có banner thuộc group nhưng không xuất hiện trong request -> xóa
     */
    BannerGroupResponse updateGroupWithBanners(BannerGroupUpdateRequest request);

    /**
     * Xóa một BannerGroup cùng các Banner con theo id
     */
    void deleteGroupById(java.util.UUID groupId);
}