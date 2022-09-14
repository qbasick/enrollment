package com.petrunkov.diskapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petrunkov.diskapi.dto.SystemItemDto;
import com.petrunkov.diskapi.dto.SystemItemHistoryResponse;
import com.petrunkov.diskapi.dto.SystemItemImport;
import com.petrunkov.diskapi.dto.SystemItemImportRequest;
import com.petrunkov.diskapi.model.SystemItem;
import com.petrunkov.diskapi.repository.ArchiveRepository;
import com.petrunkov.diskapi.repository.StorageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc

class DiskApiApplicationTests {
	@Container
	private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres");

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
		dynamicPropertyRegistry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
		dynamicPropertyRegistry.add("spring.datasource.username", postgreSQLContainer::getUsername);
		dynamicPropertyRegistry.add("spring.datasource.password", postgreSQLContainer::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StorageRepository storageRepository;

	@Autowired
	private ArchiveRepository archiveRepository;

	@BeforeEach
	void clear() {
		storageRepository.deleteAll();
		archiveRepository.deleteAll();
	}

	@Test
	void shouldImportAndDeleteSingleFile() throws Exception {
		SystemItemImport itemImport = SystemItemImport.builder()
				.id("id")
				.parentId(null)
				.size(4L)
				.type("FILE")
				.url("/first")
				.build();

		var importRequest = buildImportRequest(List.of(itemImport));

		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());
		Assertions.assertEquals(1, storageRepository.findAll().size());

		mockMvc.perform(MockMvcRequestBuilders
						.delete("/delete/id")
						.param("date", Instant.now().toString()))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Assertions.assertEquals(0, storageRepository.findAll().size());
		Assertions.assertEquals(0, archiveRepository.findAll().size());
	}

	@Test
	void invalidItemsTest() throws Exception {
		SystemItemImport idNullTest = SystemItemImport.builder()
				.id(null)
				.parentId(null)
				.size(4L)
				.type("FILE")
				.url("/first")
				.build();
		var importRequest = buildImportRequest(List.of(idNullTest));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
		Assertions.assertEquals(0, storageRepository.findAll().size());

		SystemItemImport folderUrlTest = SystemItemImport.builder()
				.id("id")
				.parentId(null)
				.size(null)
				.type("FOLDER")
				.url("/first")
				.build();
		importRequest = buildImportRequest(List.of(folderUrlTest));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
		Assertions.assertEquals(0, storageRepository.findAll().size());

		SystemItemImport folderSizeTest = SystemItemImport.builder()
				.id("id")
				.parentId(null)
				.size(4L)
				.type("FOLDER")
				.url(null)
				.build();
		importRequest = buildImportRequest(List.of(folderSizeTest));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
		Assertions.assertEquals(0, storageRepository.findAll().size());

		String str = "a";
		String longUrl = str.repeat(256);
		SystemItemImport fileUrlTest = SystemItemImport.builder()
				.id("id")
				.parentId(null)
				.size(4L)
				.type("FILE")
				.url(longUrl)
				.build();
		importRequest = buildImportRequest(List.of(fileUrlTest));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
		Assertions.assertEquals(0, storageRepository.findAll().size());

		SystemItemImport fileSizeTest = SystemItemImport.builder()
				.id("id")
				.parentId(null)
				.size(0L)
				.type("FILE")
				.url("/first")
				.build();
		importRequest = buildImportRequest(List.of(fileSizeTest));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());

		Assertions.assertEquals(0, storageRepository.findAll().size());
	}

	@Test
	void fileAsParentIdTest() throws Exception{

		// File as parentId
		SystemItemImport first = SystemItemImport.builder()
				.id("first")
				.parentId(null)
				.size(4L)
				.type("FILE")
				.url("/first")
				.build();
		SystemItemImport second = SystemItemImport.builder()
				.id("second")
				.parentId("first")
				.size(4L)
				.type("FILE")
				.url("/second")
				.build();
		var importRequest = buildImportRequest(List.of(first, second));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());

		Assertions.assertEquals(0, storageRepository.findAll().size());
		Assertions.assertEquals(0, archiveRepository.findAll().size());
	}

	@Test
	void itemTypeChangeTest() throws Exception {
		SystemItemImport first = SystemItemImport.builder()
				.id("adsadsa")
				.parentId(null)
				.size(4L)
				.type("FILE")
				.url("/gdfgdf")
				.build();
		var importRequest = buildImportRequest(List.of(first));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());
		Assertions.assertEquals(1, storageRepository.findAll().size());

		SystemItemImport second = SystemItemImport.builder()
				.id("adsadsa")
				.parentId(null)
				.size(null)
				.type("FOLDER")
				.url(null)
				.build();
		importRequest = buildImportRequest(List.of(second));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
		Assertions.assertEquals(1, storageRepository.findAll().size());
	}

	@Test
	void shouldBuildCorrectTree() throws Exception {
		SystemItemDto correctTree = buildTree();
		insertTree();

		String response = mockMvc.perform(MockMvcRequestBuilders
						.get("/nodes/1"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();

		SystemItemDto testTree = objectMapper.readValue(response, SystemItemDto.class);

		Assertions.assertTrue(areTreesEqual(correctTree, testTree));
	}

	@Test
	void testUpdates() throws Exception {
		insertTree();

		String resp = mockMvc.perform(MockMvcRequestBuilders
						.get("/updates")
						.param("date", Instant.parse("2022-05-28T18:12:01Z").toString()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();

		SystemItemHistoryResponse historyResponse = objectMapper.readValue(resp, SystemItemHistoryResponse.class);
		Assertions.assertEquals(2, historyResponse.getItems().size());
	}

	@Test
	void testNodeHistory() throws Exception{
		insertTree();
		String resp = mockMvc.perform(MockMvcRequestBuilders
						.get("/node/1/history")
						.param("dateStart", Instant.parse("2000-03-27T17:12:01Z").toString())
						.param("dateEnd", Instant.parse("2042-03-27T17:12:01Z").toString()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();
		SystemItemHistoryResponse historyResponse = objectMapper.readValue(resp, SystemItemHistoryResponse.class);
		Assertions.assertEquals(8, historyResponse.getItems().size());

		mockMvc.perform(MockMvcRequestBuilders
						.delete("/delete/5")
						.param("date", Instant.now().toString()))
				.andExpect(MockMvcResultMatchers.status().isOk());
		mockMvc.perform(MockMvcRequestBuilders
					.get("/node/8/history")
					.param("dateStart", Instant.parse("2000-03-27T17:12:01Z").toString())
					.param("dateEnd", Instant.parse("2042-03-27T17:12:01Z").toString()))
				.andExpect(MockMvcResultMatchers.status().isNotFound());

	}

	@Test
	void treeDestructionTest() throws Exception {
		insertTree();

		mockMvc.perform(MockMvcRequestBuilders
						.delete("/delete/5")
						.param("date", Instant.now().toString()))
				.andExpect(MockMvcResultMatchers.status().isOk());
		Assertions.assertEquals(6, storageRepository.findAll().size());

		mockMvc.perform(MockMvcRequestBuilders
						.delete("/delete/3")
						.param("date", Instant.now().toString()))
				.andExpect(MockMvcResultMatchers.status().isOk());
		Assertions.assertEquals(3, storageRepository.findAll().size());

		mockMvc.perform(MockMvcRequestBuilders
						.delete("/delete/1")
						.param("date", Instant.now().toString()))
				.andExpect(MockMvcResultMatchers.status().isOk());
		Assertions.assertEquals(0, storageRepository.findAll().size());

		mockMvc.perform(MockMvcRequestBuilders
						.delete("/delete/3")
						.param("date", Instant.now().toString()))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
		Assertions.assertEquals(0, archiveRepository.findAll().size());
	}

	@Test
	void repeatedItemsTest() throws Exception {
		SystemItemImport first = SystemItemImport.builder()
				.id("first")
				.parentId(null)
				.size(null)
				.type("FOLDER")
				.url(null)
				.build();
		SystemItemImport second = SystemItemImport.builder()
				.id("second")
				.parentId("first")
				.size(null)
				.type("FOLDER")
				.url(null)
				.build();
		SystemItemImport third = SystemItemImport.builder()
				.id("third")
				.parentId("first")
				.size(8L)
				.type("FILE")
				.url("/third")
				.build();
		SystemItemImport fourth = SystemItemImport.builder()
				.id("fourth")
				.parentId("second")
				.size(4L)
				.type("FILE")
				.url("/fourth")
				.build();
		var importRequest = buildImportRequest(List.of(first, second, third, fourth));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());
		Optional<SystemItem> optionalSystemItem = storageRepository.findById("first");
		Assertions.assertTrue(optionalSystemItem.isPresent());
		Assertions.assertEquals(12L, optionalSystemItem.get().getSize());


		importRequest = buildImportRequest(List.of(first, second, third, fourth));
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());
		optionalSystemItem = storageRepository.findById("first");
		Assertions.assertTrue(optionalSystemItem.isPresent());
		Assertions.assertEquals(12L, optionalSystemItem.get().getSize());
	}

	// TreeForTesting
	private SystemItemDto buildTree() {
		/*
		*           1
		*        /  |  \
		*       2   3   4
		*      /   / \
 		*     5   6   7
		*    /
		*   8
		*
		* */
		String date8 = Instant.parse("2022-05-28T18:12:01Z").toString();
		String date6 = Instant.parse("2022-05-27T19:12:01Z").toString();
		String date7 = Instant.parse("2022-04-27T16:12:01Z").toString();
		String date4 = Instant.parse("2022-03-27T17:12:01Z").toString();
		SystemItemDto item8 = SystemItemDto.builder()
				.id("8")
				.parentId("5")
				.url("/8")
				.type("FILE")
				.size(8L)
				.date(date8)
				.children(null)
				.build();
		SystemItemDto item5 = SystemItemDto.builder()
				.id("5")
				.parentId("2")
				.url(null)
				.type("FOLDER")
				.size(8L)
				.date(date8)
				.children(List.of(item8))
				.build();
		SystemItemDto item2 = SystemItemDto.builder()
				.id("2")
				.parentId("1")
				.url(null)
				.type("FOLDER")
				.size(8L)
				.date(date8)
				.children(List.of(item5))
				.build();
		SystemItemDto item6 = SystemItemDto.builder()
				.id("6")
				.parentId("3")
				.url("/6")
				.type("FILE")
				.size(4L)
				.date(date6)
				.children(null)
				.build();
		SystemItemDto item7 = SystemItemDto.builder()
				.id("7")
				.parentId("3")
				.url("/7")
				.type("FILE")
				.size(2L)
				.date(date7)
				.children(null)
				.build();
		SystemItemDto item3 = SystemItemDto.builder()
				.id("3")
				.parentId("1")
				.url(null)
				.type("FOLDER")
				.size(6L)
				.date(date6)
				.children(List.of(item6, item7))
				.build();
		SystemItemDto item4 = SystemItemDto.builder()
				.id("4")
				.parentId("1")
				.url("/4")
				.type("FILE")
				.size(10L)
				.date(date4)
				.children(null)
				.build();
		SystemItemDto item1 = SystemItemDto.builder()
				.id("1")
				.parentId(null)
				.url(null)
				.type("FOLDER")
				.size(24L)
				.date(date8)
				.children(List.of(item2, item3, item4))
				.build();
		return item1;
	}

	private void insertTree() throws Exception {
		SystemItemImport item1 = SystemItemImport.builder()
				.id("1")
				.parentId(null)
				.size(null)
				.type("FOLDER")
				.url(null)
				.build();
		SystemItemImport item4 = SystemItemImport.builder()
				.id("4")
				.parentId("1")
				.size(10L)
				.type("FILE")
				.url("/4")
				.build();
		String date4 = Instant.parse("2022-03-27T17:12:01Z").toString();
		var importRequest = buildImportRequestWithDate(List.of(item1, item4), date4);
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());



		SystemItemImport item7 = SystemItemImport.builder()
				.id("7")
				.parentId("3")
				.size(2L)
				.type("FILE")
				.url("/7")
				.build();
		SystemItemImport item3 = SystemItemImport.builder()
				.id("3")
				.parentId("1")
				.size(null)
				.type("FOLDER")
				.url(null)
				.build();
		String date7 = Instant.parse("2022-04-27T16:12:01Z").toString();
		importRequest = buildImportRequestWithDate(List.of(item3, item7), date7);
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());

		SystemItemImport item6 = SystemItemImport.builder()
				.id("6")
				.parentId("3")
				.size(4L)
				.type("FILE")
				.url("/6")
				.build();
		String date6 = Instant.parse("2022-05-27T19:12:01Z").toString();
		importRequest = buildImportRequestWithDate(List.of(item6), date6);
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());

		SystemItemImport item2 = SystemItemImport.builder()
				.id("2")
				.parentId("1")
				.size(null)
				.type("FOLDER")
				.url(null)
				.build();
		SystemItemImport item5 = SystemItemImport.builder()
				.id("5")
				.parentId("2")
				.size(null)
				.type("FOLDER")
				.url(null)
				.build();
		SystemItemImport item8 = SystemItemImport.builder()
				.id("8")
				.parentId("5")
				.size(8L)
				.type("FILE")
				.url("/8")
				.build();
		String date8 = Instant.parse("2022-05-28T18:12:01Z").toString();
		importRequest = buildImportRequestWithDate(List.of(item2, item5, item8), date8);
		mockMvc.perform(MockMvcRequestBuilders
						.post("/imports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(importRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}
	private boolean areTreesEqual(SystemItemDto first, SystemItemDto second) {
		try {
			deepSort(first);
			deepSort(second);
			return objectMapper.writeValueAsString(first).equals(objectMapper.writeValueAsString(second));
		} catch (Exception e) {
			return false;
		}
	}

	private void deepSort(SystemItemDto systemItemDto) {
		if (systemItemDto.getChildren() == null) {
			return;
		}
		systemItemDto.setChildren(new ArrayList<>(systemItemDto.getChildren()));
		systemItemDto.getChildren().sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getId(), o2.getId()));
		systemItemDto.getChildren().forEach(this::deepSort);
	}


	private SystemItemImportRequest buildImportRequest(List<SystemItemImport> importList) {
		String currentTime = Instant.now().toString();

		return new SystemItemImportRequest(importList, currentTime);
	}

	private SystemItemImportRequest buildImportRequestWithDate(List<SystemItemImport> importList, String time) {

		return new SystemItemImportRequest(importList, time);
	}


}
