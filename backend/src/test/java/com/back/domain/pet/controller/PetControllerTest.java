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
    @DisplayName("펫 수정 성공 테스트")
    void t2() throws Exception {
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
    @DisplayName("펫 삭제 성공 테스트")
    void v3() throws Exception {
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
    @DisplayName("펫 단건 조회 성공 테스트")
    void t4() throws Exception {
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
    @WithMockUser(username = "testuser@example.com")
    @DisplayName("펫 전체 조회 성공 테스트")
    void t5() throws Exception {
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



}