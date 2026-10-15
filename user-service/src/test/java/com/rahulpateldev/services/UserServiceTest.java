package com.rahulpateldev.services;

import com.rahulpateldev.entities.Post;
import com.rahulpateldev.entities.User;
import com.rahulpateldev.exceptions.custom.IDNotFoundException;
import com.rahulpateldev.external.service.PostService;
import com.rahulpateldev.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-123")
                .username("testuser")
                .password("secret")
                .gender("M")
                .age(30)
                .build();
    }

    @Test
    void saveUser_shouldAssignIdAndPersist() {
        User input = User.builder()
                .username("newuser")
                .password("pass")
                .gender("F")
                .age(25)
                .build();

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.saveUser(input);

        assertThat(result.getId()).isNotNull().isNotBlank();
        verify(userRepository, times(1)).save(input);
    }

    @Test
    void getUsers_whenUserExists_shouldReturnUserWithPosts() {
        Post post = Post.builder().id("post-1").name("Post Title").description("Post Description").build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(sampleUser));
        when(postService.getPostsByUserId("user-123")).thenReturn(List.of(post));

        User result = userService.getUsers("user-123");

        assertThat(result.getId()).isEqualTo("user-123");
        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getName()).isEqualTo("Post Title");
    }

    @Test
    void getUsers_whenUserNotFound_shouldThrowIDNotFoundException() {
        when(userRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUsers("bad-id"))
                .isInstanceOf(IDNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAllUsers_shouldReturnListWithPostsPopulated() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        when(postService.getPostsByUserId("user-123")).thenReturn(Collections.emptyList());

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void deleteUser_shouldCallRepositoryDeleteById() {
        doNothing().when(userRepository).deleteById("user-123");

        userService.deleteUser("user-123");

        verify(userRepository, times(1)).deleteById("user-123");
    }

    @Test
    void updateUser_shouldUpdateAgeAndGender() {
        User updatedData = User.builder().age(35).gender("F").build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(sampleUser));
        when(postService.getPostsByUserId("user-123")).thenReturn(Collections.emptyList());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser("user-123", updatedData);

        assertThat(result.getAge()).isEqualTo(35);
        assertThat(result.getGender()).isEqualTo("F");
    }
}
