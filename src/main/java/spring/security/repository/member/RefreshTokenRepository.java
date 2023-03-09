package spring.security.repository.member;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.security.domain.member.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByKey(String key);
}
