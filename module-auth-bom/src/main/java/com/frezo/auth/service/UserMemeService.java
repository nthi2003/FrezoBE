package com.frezo.auth.service;

import com.frezo.auth.entity.UserMeme;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserMemeService {

    List<UserMeme> listMine(String username);

    UserMeme upload(String username, MultipartFile file, String title);

    void delete(String username, String memeId);
}
