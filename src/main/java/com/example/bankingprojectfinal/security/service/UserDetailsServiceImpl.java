package com.example.bankingprojectfinal.security.service;

import com.example.bankingprojectfinal.Exception.UserNameNotFoundException;
import com.example.bankingprojectfinal.Model.Entity.UserEntity;
import com.example.bankingprojectfinal.Repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Supplier<? extends Throwable> UserNameNotFoundException = null;
        UserEntity userEntity = null;
        try {
            userEntity = userRepository.findById(username).orElseThrow(UserNameNotFoundException);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority("ROLE_" + userEntity.getRole().name());

        return new User(
                userEntity.getUsername(),
                userEntity.getPassword(),
                Collections.singletonList(simpleGrantedAuthority)
        );
    }
}
