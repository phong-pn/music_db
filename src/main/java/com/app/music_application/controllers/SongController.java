package com.app.music_application.controllers;

import com.app.music_application.models.Category;
import com.app.music_application.models.ResponseObject;
import com.app.music_application.models.Song;
import com.app.music_application.models.User;
import com.app.music_application.repositories.CategoryRepository;
import com.app.music_application.repositories.SongRepository;
import com.app.music_application.repositories.UserRepository;
import com.app.music_application.services.ImageStorageService;
import com.app.music_application.services.SongStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/Songs")
public class SongController {
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ImageStorageService imageStorageService;
    @Autowired
    private SongStorageService songStorageService;

    @GetMapping("/ShowAll")
    List<Song> getAllSongs() {return songRepository.findAll();}

    @GetMapping("/{id}")
    ResponseEntity<ResponseObject> findById(@PathVariable Long id) {
        Optional<Song> foundSong = songRepository.findById(id);
        return foundSong.isPresent() ?
                ResponseEntity.status(HttpStatus.OK).body(
                        new ResponseObject("OK", "Find song successfully", foundSong)
                ):
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ResponseObject("False","Cannot find song with id ="+id,foundSong)
                );
    }

    @GetMapping("/imageFiles/{fileName:.+}")
    public ResponseEntity<byte[]> readDetailImageFile(@PathVariable String fileName) {
        try {
            byte[] bytes = imageStorageService.readFileContent(fileName);
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(bytes);

        }catch (Exception exception){
            return ResponseEntity.noContent().build(); // ko tìm thấy image trả về nocontent
        }
    }

    @GetMapping("/musicFiles/{fileName:.+}")
    public ResponseEntity<byte[]> readDetailSongFile(@PathVariable String fileName) {
        // Xử lý logic để đọc file chi tiết dựa trên fileName
        // ...
        try {
            byte[] bytes = songStorageService.readFileContent(fileName);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(bytes);
        }
        catch (Exception exception) {
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping("/insert")
    ResponseEntity<ResponseObject> insertSong(@RequestParam("name") String name,
                                              @RequestParam("image")MultipartFile imagefile,
                                              @RequestParam("song")MultipartFile songfile,
                                              @RequestParam("category") Long categoryId,
                                              @RequestParam("creator") Long userId) {
            // lưu trữ file
            String songFileName = songStorageService.storeFile(songfile);
            String imageFileName = imageStorageService.storeFile(imagefile);
            //lấy đường dẫn Url
            String songUrl = MvcUriComponentsBuilder.fromMethodName(SongController.class,
                    "readDetailSongFile", songFileName).build().toUri().toString();
            String urlImage = MvcUriComponentsBuilder.fromMethodName(SongController.class,
                    "readDetailImageFile", imageFileName).build().toUri().toString();

//        Optional<User> optionalUser = userRepository.findById(userId);
//        if (optionalUser.isEmpty()) {
//            // Xử lý khi không tìm thấy user
//            return ResponseEntity.notFound().build();
//        }
//        User creator = optionalUser.get();
//
//        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);
//        if (optionalCategory.isEmpty()) {
//            // Xử lý khi không tìm thấy category
//            return ResponseEntity.notFound().build();
//        }
//        Category category = optionalCategory.get();
            User creator = userRepository.findById(userId).orElse(null);
            Category category = categoryRepository.findById(categoryId).orElse(null);

            Song song = new Song();
            song.setName(name);
            song.setUrl(songUrl);
            song.setThumbnailUrl(urlImage);
            song.setCategory(category);
            song.setCreatorId(creator);
            song.setDownloadCount(0);
            song.setCreatedAt(LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "Insert song successfully", songRepository.save(song))
            );
    }

    // update, upsert = update if found, otherwise insert
    @PutMapping("/{id}")
    public ResponseEntity<ResponseObject>  updateProduct(@RequestBody Song newSong, @PathVariable Long id) {
        Song updateSong = songRepository.findById(id)
                .map(song -> {
                   song.setName(newSong.getName());
                   song.setCategory(newSong.getCategory());
                   song.setThumbnailUrl(newSong.getThumbnailUrl());
                   return songRepository.save(song);
                }).orElseGet(() ->{
                    newSong.setId(id);
                    return songRepository.save(newSong);
                });
        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("OK","Update song successfully", updateSong)
        );
    }
    @PutMapping ("/{id}/download")
    public ResponseEntity<ResponseObject> updateDownloadCount(@PathVariable Long id) {
        // Tìm bài hát theo songId trong cơ sở dữ liệu
        Optional<Song> optionalSong = songRepository.findById(id);
        if (optionalSong.isPresent()) {
            Song song = optionalSong.get();
            // Tăng giá trị downloadCount
            song.setDownloadCount(song.getDownloadCount() + 1);
            // Lưu đối tượng Song đã được cập nhật
            Song updatedSong = songRepository.save(song);
            return ResponseEntity.ok().body(new ResponseObject("ok", "Download count updated", updatedSong));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //Delete a product -> DELETE method
    @DeleteMapping("/{id}")
    ResponseEntity<ResponseObject> deleteSong(@PathVariable Long id) {
        boolean exists = songRepository.existsById(id);
        if(exists){
            songRepository.deleteById(id);
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "delete song successfully","")
            );
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ResponseObject("failed", "cannot find song to delete","")
        );
    }

}
