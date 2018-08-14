package org.exampleapps.greatbig.web.rest;

import org.exampleapps.greatbig.GreatBigExampleApplicationApp;

import org.exampleapps.greatbig.domain.Crisis;
import org.exampleapps.greatbig.repository.CrisisRepository;
import org.exampleapps.greatbig.repository.search.CrisisSearchRepository;
import org.exampleapps.greatbig.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;


import static org.exampleapps.greatbig.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the CrisisResource REST controller.
 *
 * @see CrisisResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = GreatBigExampleApplicationApp.class)
public class CrisisResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    @Autowired
    private CrisisRepository crisisRepository;


    /**
     * This repository is mocked in the org.exampleapps.greatbig.repository.search test package.
     *
     * @see org.exampleapps.greatbig.repository.search.CrisisSearchRepositoryMockConfiguration
     */
    @Autowired
    private CrisisSearchRepository mockCrisisSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restCrisisMockMvc;

    private Crisis crisis;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final CrisisResource crisisResource = new CrisisResource(crisisRepository, mockCrisisSearchRepository);
        this.restCrisisMockMvc = MockMvcBuilders.standaloneSetup(crisisResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Crisis createEntity(EntityManager em) {
        Crisis crisis = new Crisis()
            .name(DEFAULT_NAME);
        return crisis;
    }

    @Before
    public void initTest() {
        crisis = createEntity(em);
    }

    @Test
    @Transactional
    public void createCrisis() throws Exception {
        int databaseSizeBeforeCreate = crisisRepository.findAll().size();

        // Create the Crisis
        restCrisisMockMvc.perform(post("/api/crises")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(crisis)))
            .andExpect(status().isCreated());

        // Validate the Crisis in the database
        List<Crisis> crisisList = crisisRepository.findAll();
        assertThat(crisisList).hasSize(databaseSizeBeforeCreate + 1);
        Crisis testCrisis = crisisList.get(crisisList.size() - 1);
        assertThat(testCrisis.getName()).isEqualTo(DEFAULT_NAME);

        // Validate the Crisis in Elasticsearch
        verify(mockCrisisSearchRepository, times(1)).save(testCrisis);
    }

    @Test
    @Transactional
    public void createCrisisWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = crisisRepository.findAll().size();

        // Create the Crisis with an existing ID
        crisis.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restCrisisMockMvc.perform(post("/api/crises")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(crisis)))
            .andExpect(status().isBadRequest());

        // Validate the Crisis in the database
        List<Crisis> crisisList = crisisRepository.findAll();
        assertThat(crisisList).hasSize(databaseSizeBeforeCreate);

        // Validate the Crisis in Elasticsearch
        verify(mockCrisisSearchRepository, times(0)).save(crisis);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = crisisRepository.findAll().size();
        // set the field null
        crisis.setName(null);

        // Create the Crisis, which fails.

        restCrisisMockMvc.perform(post("/api/crises")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(crisis)))
            .andExpect(status().isBadRequest());

        List<Crisis> crisisList = crisisRepository.findAll();
        assertThat(crisisList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllCrises() throws Exception {
        // Initialize the database
        crisisRepository.saveAndFlush(crisis);

        // Get all the crisisList
        restCrisisMockMvc.perform(get("/api/crises?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisis.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())));
    }
    

    @Test
    @Transactional
    public void getCrisis() throws Exception {
        // Initialize the database
        crisisRepository.saveAndFlush(crisis);

        // Get the crisis
        restCrisisMockMvc.perform(get("/api/crises/{id}", crisis.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(crisis.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()));
    }
    @Test
    @Transactional
    public void getNonExistingCrisis() throws Exception {
        // Get the crisis
        restCrisisMockMvc.perform(get("/api/crises/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCrisis() throws Exception {
        // Initialize the database
        crisisRepository.saveAndFlush(crisis);

        int databaseSizeBeforeUpdate = crisisRepository.findAll().size();

        // Update the crisis
        Crisis updatedCrisis = crisisRepository.findById(crisis.getId()).get();
        // Disconnect from session so that the updates on updatedCrisis are not directly saved in db
        em.detach(updatedCrisis);
        updatedCrisis
            .name(UPDATED_NAME);

        restCrisisMockMvc.perform(put("/api/crises")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedCrisis)))
            .andExpect(status().isOk());

        // Validate the Crisis in the database
        List<Crisis> crisisList = crisisRepository.findAll();
        assertThat(crisisList).hasSize(databaseSizeBeforeUpdate);
        Crisis testCrisis = crisisList.get(crisisList.size() - 1);
        assertThat(testCrisis.getName()).isEqualTo(UPDATED_NAME);

        // Validate the Crisis in Elasticsearch
        verify(mockCrisisSearchRepository, times(1)).save(testCrisis);
    }

    @Test
    @Transactional
    public void updateNonExistingCrisis() throws Exception {
        int databaseSizeBeforeUpdate = crisisRepository.findAll().size();

        // Create the Crisis

        // If the entity doesn't have an ID, it will throw BadRequestAlertException 
        restCrisisMockMvc.perform(put("/api/crises")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(crisis)))
            .andExpect(status().isBadRequest());

        // Validate the Crisis in the database
        List<Crisis> crisisList = crisisRepository.findAll();
        assertThat(crisisList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Crisis in Elasticsearch
        verify(mockCrisisSearchRepository, times(0)).save(crisis);
    }

    @Test
    @Transactional
    public void deleteCrisis() throws Exception {
        // Initialize the database
        crisisRepository.saveAndFlush(crisis);

        int databaseSizeBeforeDelete = crisisRepository.findAll().size();

        // Get the crisis
        restCrisisMockMvc.perform(delete("/api/crises/{id}", crisis.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Crisis> crisisList = crisisRepository.findAll();
        assertThat(crisisList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Crisis in Elasticsearch
        verify(mockCrisisSearchRepository, times(1)).deleteById(crisis.getId());
    }

    @Test
    @Transactional
    public void searchCrisis() throws Exception {
        // Initialize the database
        crisisRepository.saveAndFlush(crisis);
        when(mockCrisisSearchRepository.search(queryStringQuery("id:" + crisis.getId())))
            .thenReturn(Collections.singletonList(crisis));
        // Search the crisis
        restCrisisMockMvc.perform(get("/api/_search/crises?query=id:" + crisis.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisis.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Crisis.class);
        Crisis crisis1 = new Crisis();
        crisis1.setId(1L);
        Crisis crisis2 = new Crisis();
        crisis2.setId(crisis1.getId());
        assertThat(crisis1).isEqualTo(crisis2);
        crisis2.setId(2L);
        assertThat(crisis1).isNotEqualTo(crisis2);
        crisis1.setId(null);
        assertThat(crisis1).isNotEqualTo(crisis2);
    }
}
