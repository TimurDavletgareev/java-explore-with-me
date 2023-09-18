package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.error.exception.ConflictOnRequestException;
import ru.practicum.ewm.error.exception.IncorrectRequestException;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserDto;
import ru.practicum.ewm.user.dto.UserFullDto;
import ru.practicum.ewm.user.dto.UserMapper;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;

    public UserFullDto add(NewUserDto newUserDto) {

        log.info("-- Сохранение пользователя:{}", newUserDto);

        User user = UserMapper.newUserDtoToUser(newUserDto);

        try {

            UserFullDto fullUserDtoToReturn = UserMapper.userToFullDto(userRepository.save(user));

            log.info("-- Пользователь сохранён: {}", fullUserDtoToReturn);

            return fullUserDtoToReturn;

        } catch (DataIntegrityViolationException e) {

            throw new ConflictOnRequestException("- Такой адрес почты уже есть в базе, пользователь не сохранён");
        }
    }

    public List<UserFullDto> get(Long[] userIds, int from, int size) {

        log.info("-- Возвращение пользователей с номерами:{}", Arrays.toString(userIds));

        PageRequest pageRequest;

        if (size > 0 && from >= 0) {
            int page = from / size;
            pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
        } else {
            throw new IncorrectRequestException("- Размер страницы должен быть > 0, 'from' должен быть >= 0");
        }

        List<UserFullDto> listToReturn = UserMapper.userToFullDto(userRepository.findByIdIn(userIds, pageRequest));

        log.info("-- Список пользователей возвращен, его размер: {}", listToReturn.size());

        return listToReturn;
    }

    public void removeById(Long userId) {

        log.info("--- Удаление пользователя №{}", userId);

        User userToCheck = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("- Пользователь №" + userId + " не найден в базе"));

        UserShortDto userToShowInLog = UserMapper.userToShortDto(userToCheck);

        userRepository.deleteById(userId);

        log.info("--- Пользователь удален: {}", userToShowInLog);
    }
}