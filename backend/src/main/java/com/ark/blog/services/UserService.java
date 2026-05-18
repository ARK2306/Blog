package com.ark.blog.services;

import com.ark.blog.domain.entities.Post;
import com.ark.blog.domain.entities.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    User getUserById(UUID id);

}
