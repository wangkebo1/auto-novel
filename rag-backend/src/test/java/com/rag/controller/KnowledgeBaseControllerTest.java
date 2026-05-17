package com.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.dto.KnowledgeBaseRequest;
import com.rag.entity.KnowledgeBase;
import com.rag.service.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KnowledgeBaseController.class)
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    void list_Success() throws Exception {
        KnowledgeBase kb1 = new KnowledgeBase();
        kb1.setId(1L);
        kb1.setName("知识库1");

        KnowledgeBase kb2 = new KnowledgeBase();
        kb2.setId(2L);
        kb2.setName("知识库2");

        List<KnowledgeBase> list = Arrays.asList(kb1, kb2);
        when(knowledgeBaseService.listAll()).thenReturn(list);

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void create_Success() throws Exception {
        KnowledgeBaseRequest request = new KnowledgeBaseRequest();
        request.setName("新知识库");
        request.setDescription("测试描述");

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setName("新知识库");

        when(knowledgeBaseService.create(any(KnowledgeBaseRequest.class))).thenReturn(kb);

        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("新知识库"));
    }

    @Test
    void getById_Success() throws Exception {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setName("知识库1");

        when(knowledgeBaseService.getById(1L)).thenReturn(kb);

        mockMvc.perform(get("/api/knowledge-bases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
