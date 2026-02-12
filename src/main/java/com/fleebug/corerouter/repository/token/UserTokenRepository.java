package com.fleebug.corerouter.repository.token;

import com.fleebug.corerouter.model.token.UserToken;
import com.fleebug.corerouter.model.user.User;
import com.fleebug.corerouter.enums.token.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    List<UserToken> findByUser(User user);

    List<UserToken> findByUserAndTokenType(User user, TokenType tokenType);

    Optional<UserToken> findByTokenValue(String tokenValue);

    List<UserToken> findByUserAndRevokedFalse(User user);

    List<UserToken> findByExpiresAtBefore(LocalDateTime now);
}
