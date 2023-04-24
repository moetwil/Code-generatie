package w.mazebank.services;

import w.mazebank.models.User;
import w.mazebank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User addUser(User user){
        userRepository.save(user);
        return user;
    }
}
