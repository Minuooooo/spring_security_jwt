package spring.security.repository.member;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.security.domain.member.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
}
