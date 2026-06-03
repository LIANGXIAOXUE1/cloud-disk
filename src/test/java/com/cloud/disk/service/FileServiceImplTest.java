package com.cloud.disk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.disk.repository.entity.FileInfo;
import com.cloud.disk.repository.mapper.FileInfoMapper;
import com.cloud.disk.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FileServiceImpl core logic.
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileInfoMapper fileInfoMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus ServiceImpl internally uses baseMapper
        fileService = new FileServiceImpl() {
            @Override
            public FileInfoMapper getBaseMapper() {
                return fileInfoMapper;
            }
        };
    }

    @Test
    void testGetFileExtension() throws Exception {
        // Use reflection to call private method for coverage
        var method = FileServiceImpl.class.getDeclaredMethod("getFileExtension", String.class);
        method.setAccessible(true);

        assertEquals("pdf", method.invoke(fileService, "test.pdf"));
        assertEquals("docx", method.invoke(fileService, "file.docx"));
        assertEquals("", method.invoke(fileService, "nofile"));
        assertEquals("", method.invoke(fileService, (String) null));
    }

    @Test
    void testIsImageFile() throws Exception {
        var method = FileServiceImpl.class.getDeclaredMethod("isImageFile", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(fileService, "photo.jpg"));
        assertTrue((Boolean) method.invoke(fileService, "icon.png"));
        assertFalse((Boolean) method.invoke(fileService, "doc.pdf"));
        assertFalse((Boolean) method.invoke(fileService, (String) null));
    }

    @Test
    void testListByParentId_ReturnsEmptyWhenNoFiles() {
        when(fileInfoMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        var result = fileService.listByParentId(1L, null, 1, 20);
        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testExistsSameName_NoMatch() {
        when(fileInfoMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertFalse(fileService.existsSameName(1L, null, "newfile.txt", null));
    }

    @Test
    void testExistsSameName_MatchFound() {
        when(fileInfoMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertTrue(fileService.existsSameName(1L, null, "duplicate.txt", null));
    }
}
