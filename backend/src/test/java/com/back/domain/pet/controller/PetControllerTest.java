package com.back.domain.pet.controller;

import com.back.domain.member.entity.Member;
import com.back.domain.member.enums.UserRole;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.pet.dto.request.PetCreateRequestDto;
import com.back.domain.pet.dto.request.PetUpdateRequestDto;
import com.back.domain.pet.enums.Gender;
import com.back.domain.pet.repository.PetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        petRepository.deleteAll();
        memberRepository.deleteAll();

        testMember = Member.builder()
                .email("testuser@example.com")
                .name("테스트 유저")
                .password("encoded-password")
                .phone("010-1234-5678")  // 여기 꼭 추가
                .role(UserRole.USER)
                .build();
        memberRepository.save(testMember);

        otherMember = Member.builder()
                .email("otheruser@example.com")
                .name("다른 유저")
                .password("encoded-password")
                .phone("010-9876-5432")
                .role(UserRole.USER)
                .build();
        memberRepository.save(otherMember);
    }


    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 생성 성공 테스트")
    void t1() throws Exception {
        PetCreateRequestDto dto = new PetCreateRequestDto(
                "초코",
                "푸들",
                3,
                Gender.FEMALE,
                "활발한 강아지",
                "http://example.com/image.jpg",
                "",
                List.of("ADOPTED", "CARE_IN_PROGRESS")
        );

        mockMvc.perform(post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.name").value("초코"))
                .andExpect(jsonPath("$.content.species").value("푸들"))
                .andExpect(jsonPath("$.content.age").value(3))
                .andExpect(jsonPath("$.content.gender").value("FEMALE"))
                .andExpect(jsonPath("$.content.description").value("활발한 강아지"))
                .andExpect(jsonPath("$.content.imageUrl").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$.content.shelterName").value("보호소 정보 없음"))
                .andExpect(jsonPath("$.content.petStatuses", hasSize(2)))
                .andExpect(jsonPath("$.content.petStatuses", containsInAnyOrder("ADOPTED", "CARE_IN_PROGRESS")));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 생성 실패 테스트 - 필수 필드 누락")
    void t2() throws Exception {
        // 이름(name) 필드 빈 문자열로 해서 유효성 실패 유도
        PetCreateRequestDto dto = new PetCreateRequestDto(
                "",  // 이름 빈 값 - 필수
                "푸들",
                3,
                Gender.FEMALE,
                "활발한 강아지",
                "http://example.com/image.jpg",
                "",
                List.of("ADOPTED")
        );

        mockMvc.perform(post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INPUT-400"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.content").isEmpty());
    }


    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 수정 성공 테스트")
    void t3() throws Exception {
        // 펫 생성 (DB 저장)
        var pet = petRepository.save(
                com.back.domain.pet.entity.Pet.builder()
                        .name("초코")
                        .species("푸들")
                        .age(3)
                        .gender(Gender.FEMALE)
                        .description("활발한 강아지")
                        .imageUrl("http://example.com/image.jpg")
                        .member(testMember)
                        .petStatuses(new ArrayList<>())
                        .build()
        );

        PetUpdateRequestDto updateDto = new PetUpdateRequestDto(
                "초코-수정",
                "푸들",
                4,
                Gender.FEMALE,
                "더 활발해진 강아지",
                "http://example.com/newimage.jpg",
                "",
                List.of("ADOPTED", "CARE_IN_PROGRESS")
        );

        mockMvc.perform(put("/api/pets/{id}", pet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.name").value("초코-수정"))
                .andExpect(jsonPath("$.content.age").value(4))
                .andExpect(jsonPath("$.content.description").value("더 활발해진 강아지"))
                .andExpect(jsonPath("$.content.imageUrl").value("http://example.com/newimage.jpg"))
                .andExpect(jsonPath("$.content.shelterName").value("보호소 정보 없음"))
                .andExpect(jsonPath("$.content.petStatuses", hasSize(0)));  // 빈 리스트로 기대

    }


    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 수정 실패 - 존재하지 않는 펫 ID")
    void t4() throws Exception {
        PetUpdateRequestDto updateDto = new PetUpdateRequestDto(
                "초코-수정",
                "푸들",
                4,
                Gender.FEMALE,
                "더 활발해진 강아지",
                "http://example.com/newimage.jpg",
                "",
                List.of("ADOPTED", "CARE_IN_PROGRESS")
        );

        mockMvc.perform(put("/api/pets/{id}", 9999L)  // 없는 ID
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PET-404"))
                .andExpect(jsonPath("$.message").value("해당 동물을 찾을 수 없습니다."));
    }

    @Test
    @WithMockUser(username = "otheruser@example.com")
    @DisplayName("펫 수정 실패 - 권한 없음 ")
    void t5() throws Exception {
        // testMember가 만든 펫 저장
        var pet = petRepository.save(
                com.back.domain.pet.entity.Pet.builder()
                        .name("초코")
                        .species("푸들")
                        .age(3)
                        .gender(Gender.FEMALE)
                        .description("활발한 강아지")
                        .imageUrl("http://example.com/image.jpg")
                        .member(testMember)  // 실제 소유자
                        .petStatuses(new ArrayList<>())
                        .build()
        );

        PetUpdateRequestDto updateDto = new PetUpdateRequestDto(
                "수정된 이름",
                "푸들",
                4,
                Gender.FEMALE,
                "수정된 설명",
                "http://example.com/newimage.jpg",
                "",
                List.of()
        );

        mockMvc.perform(put("/api/pets/{id}", pet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden())  // 403 기대
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-403"))
                .andExpect(jsonPath("$.message").value("권한이 없습니다."));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 생성 실패 - 이름 빈칸으로 인한 validation 실패 (400 Bad Request)")
    void t6() throws Exception {
        PetCreateRequestDto invalidDto = new PetCreateRequestDto(
                "",  // 빈 이름 (Invalid)
                "푸들",
                3,
                Gender.FEMALE,
                "설명",
                "http://example.com/image.jpg",
                "",
                List.of()
        );

        mockMvc.perform(post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())  // 400 기대
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INPUT-400"))
                .andExpect(jsonPath("$.message").value("이름은 필수입니다."));
    }



    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 삭제 성공 테스트")
    void t7() throws Exception {
        var pet = petRepository.save(
                com.back.domain.pet.entity.Pet.builder()
                        .name("초코")
                        .species("푸들")
                        .age(3)
                        .gender(Gender.FEMALE)
                        .description("활발한 강아지")
                        .imageUrl("http://example.com/image.jpg")
                        .member(testMember)
                        .petStatuses(new ArrayList<>())
                        .build()
        );

        mockMvc.perform(delete("/api/pets/{id}", pet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 삭제 후 데이터가 없는지 확인
        boolean exists = petRepository.existsById(pet.getId());
        assertFalse(exists);
    }
    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 삭제 실패 - 존재하지 않는 펫 ID로 삭제 시도 시 404 NOT FOUND")
    void t8() throws Exception {
        Long nonExistentPetId = 999999L; // DB에 없는 ID

        mockMvc.perform(delete("/api/pets/{id}", nonExistentPetId))
                .andExpect(status().isNotFound())  // 404 기대
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PET-404"))
                .andExpect(jsonPath("$.message").value("해당 동물을 찾을 수 없습니다."));
    }



    @Test
    @WithMockUser(username = "otheruser@example.com")  // 본인 아닌 다른 유저
    @DisplayName("펫 삭제 실패 - 권한 없는 사용자가 펫 삭제 시도 시 실패 테스트")
    void t9() throws Exception {
        var pet = petRepository.save(
                com.back.domain.pet.entity.Pet.builder()
                        .name("초코")
                        .species("푸들")
                        .age(3)
                        .gender(Gender.FEMALE)
                        .description("활발한 강아지")
                        .imageUrl("http://example.com/image.jpg")
                        .member(testMember)  // testMember가 주인
                        .petStatuses(new ArrayList<>())
                        .build()
        );

        mockMvc.perform(delete("/api/pets/{id}", pet.getId()))
                .andExpect(status().isForbidden())  // 403 에러 예상
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-403"))
                .andExpect(jsonPath("$.message").value("권한이 없습니다."));
    }


    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 단건 조회 성공 테스트")
    void t10() throws Exception {
        var pet = petRepository.save(
                com.back.domain.pet.entity.Pet.builder()
                        .name("초코")
                        .species("푸들")
                        .age(3)
                        .gender(Gender.FEMALE)
                        .description("활발한 강아지")
                        .imageUrl("http://example.com/image.jpg")
                        .member(testMember)
                        .petStatuses(new ArrayList<>())
                        .build()
        );

        mockMvc.perform(get("/api/pets/{id}", pet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.name").value("초코"))
                .andExpect(jsonPath("$.content.species").value("푸들"))
                .andExpect(jsonPath("$.content.age").value(3))
                .andExpect(jsonPath("$.content.gender").value("FEMALE"))
                .andExpect(jsonPath("$.content.description").value("활발한 강아지"))
                .andExpect(jsonPath("$.content.imageUrl").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$.content.shelterName").value("보호소 정보 없음"))
                .andExpect(jsonPath("$.content.petStatuses", hasSize(0)));
    }

    @Test
    @DisplayName("펫 단건 조회 실패 - 없는 ID")
    void t11() throws Exception {
        mockMvc.perform(get("/api/pets/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PET-404"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 전체 조회 성공 테스트")
    void t12() throws Exception {
        petRepository.saveAll(List.of(
                com.back.domain.pet.entity.Pet.builder()
                        .name("초코")
                        .species("푸들")
                        .age(3)
                        .gender(Gender.FEMALE)
                        .description("활발한 강아지")
                        .imageUrl("http://example.com/image1.jpg")
                        .member(testMember)
                        .petStatuses(new ArrayList<>())
                        .build(),
                com.back.domain.pet.entity.Pet.builder()
                        .name("콩이")
                        .species("시추")
                        .age(5)
                        .gender(Gender.MALE)
                        .description("귀여운 강아지")
                        .imageUrl("http://example.com/image2.jpg")
                        .member(testMember)
                        .petStatuses(new ArrayList<>())
                        .build()
        ));

        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("초코"))
                .andExpect(jsonPath("$.content[1].name").value("콩이"));
    }

    @Test
    @DisplayName("펫 전체 조회 - 여러 개")
    void t13() throws Exception {
        petRepository.saveAll(List.of(
                com.back.domain.pet.entity.Pet.builder()
                        .name("초코")
                        .species("푸들")
                        .age(3)
                        .gender(Gender.FEMALE)
                        .description("활발한 강아지")
                        .imageUrl("http://example.com/image1.jpg")
                        .member(testMember)
                        .petStatuses(new ArrayList<>())
                        .build(),
                com.back.domain.pet.entity.Pet.builder()
                        .name("콩이")
                        .species("시추")
                        .age(5)
                        .gender(Gender.MALE)
                        .description("귀여운 강아지")
                        .imageUrl("http://example.com/image2.jpg")
                        .member(testMember)
                        .petStatuses(new ArrayList<>())
                        .build()
        ));


        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
    }

}