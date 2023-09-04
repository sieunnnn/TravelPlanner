package travelplanner.project.demo.member.socialauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import travelplanner.project.demo.member.Member;
import travelplanner.project.demo.member.MemberRepository;
import travelplanner.project.demo.member.auth.Role;
import travelplanner.project.demo.member.profile.Profile;
import travelplanner.project.demo.member.profile.ProfileRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private final MemberRepository memberRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private @Lazy PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(request);
        log.info("------------------ getAttributes : {}", oAuth2User.getAttributes());

        String provider = request.getClientRegistration().getRegistrationId();

        // 어떤 소셜 로그인인지 구분
        OAuth2UserInfo oAuth2UserInfo = OAuthUserInfoFactory.getOAuthUserInfo(provider, oAuth2User.getAttributes());

        // 회원가입 유무 확인
        Optional<Member> member = memberRepository.findByEmail(oAuth2UserInfo.getEmail());

        // 없다면 회원가입
        if(member.isEmpty()) {

            // 멤버 생성 및 저장
            Member  newMember = Member.builder()
                    .userNickname(oAuth2UserInfo.getName())
                    .email(oAuth2UserInfo.getEmail())
                    .provider(provider)
                    .password(passwordEncoder.encode("oauth"))
                    .role(Role.MEMBER)
                    .build();

            memberRepository.save(newMember);  // 변경된 user 저장

            // 프로필 생성
            Profile profile = Profile.builder()
                    .keyName("")
                    .profileImgUrl(oAuth2UserInfo.getProfile())
                    .member(newMember)
                    .build();

            profileRepository.save(profile);  // profile 저장

            return new PrincipalDetails(newMember, oAuth2User.getAttributes());
        }

        return new PrincipalDetails(member.get(), oAuth2User.getAttributes());
    }

}
