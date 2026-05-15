package com.example.blogger.mapper;

import com.example.blogger.entity.ImageLibrary;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ImageLibraryMapper {

    @Select("<script>" +
            "SELECT * FROM tu_image_library WHERE 1=1 " +
            "<if test='categories != null and categories.size > 0'>" +
            " AND (" +
            "<foreach collection='categories' item='c' separator=' OR '>" +
            " categories LIKE CONCAT('%\"', #{c}, '\"%') " +
            "</foreach>" +
            " )" +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<ImageLibrary> findAll(@Param("categories") List<String> categories, @Param("keyword") String keyword, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tu_image_library WHERE 1=1 " +
            "<if test='categories != null and categories.size > 0'>" +
            " AND (" +
            "<foreach collection='categories' item='c' separator=' OR '>" +
            " categories LIKE CONCAT('%\"', #{c}, '\"%') " +
            "</foreach>" +
            " )" +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    int countAll(@Param("categories") List<String> categories, @Param("keyword") String keyword);

    @Select("SELECT * FROM tu_image_library WHERE id = #{id}")
    ImageLibrary findById(String id);

    @Select("SELECT * FROM tu_image_library WHERE name = #{name} LIMIT 1")
    ImageLibrary findByName(@Param("name") String name);

    @Select("SELECT * FROM tu_image_library WHERE categories LIKE CONCAT('%\"', #{trackId}, '\"%') ORDER BY RAND() LIMIT 1")
    ImageLibrary findRandomByTrackId(@Param("trackId") String trackId);

    @Insert("INSERT INTO tu_image_library(id, name, url, categories, created_at) " +
            "VALUES(#{id}, #{name}, #{url}, #{categories}, NOW())")
    int insert(ImageLibrary image);

    @Delete("DELETE FROM tu_image_library WHERE id = #{id}")
    int delete(String id);

    @Update("UPDATE tu_image_library SET name = #{name}, url = #{url}, categories = #{categories} WHERE id = #{id}")
    int update(ImageLibrary image);
}
