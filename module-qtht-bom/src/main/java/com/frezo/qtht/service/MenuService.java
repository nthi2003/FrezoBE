package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.MenuSaveRequest;
import com.frezo.qtht.dto.response.MenuResponse;
import com.frezo.qtht.entity.Menu;
import java.util.List;

public interface MenuService {
    List<MenuResponse> getMenusForUser(String username);

    List<MenuResponse> getAllMenus();
    Menu getById(String id);
    MenuResponse create(MenuSaveRequest request);
    MenuResponse update(String id, MenuSaveRequest request);

    void delete(String id);
}
