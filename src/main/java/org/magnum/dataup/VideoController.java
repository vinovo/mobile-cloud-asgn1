package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class VideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);
    private Map<Long, Video> videos = new HashMap<>();
    private VideoFileManager videoFileManager = VideoFileManager.get();

    public VideoController() throws IOException {
    }

    @RequestMapping(value = "/video", method = RequestMethod.GET)
    public @ResponseBody Collection<Video> getVideos() {

        return videos.values();
    }

    @RequestMapping(value = "/video", method = RequestMethod.POST)
    public @ResponseBody Video createVideo(@RequestBody Video video) {

        return save(video);
    }

    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
    public @ResponseBody VideoStatus uploadVideo(@PathVariable long id,
                                                 @RequestParam("data") MultipartFile videoData,
                                                 HttpServletResponse response) throws IOException {
        Video video = videos.get(id);

        if (video == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        videoFileManager.saveVideoData(video, videoData.getInputStream());
        return new VideoStatus(VideoStatus.VideoState.READY);
    }

    @RequestMapping(value= "/video/{id}/data", method = RequestMethod.GET)
    public void downloadVideo(@PathVariable long id, HttpServletResponse response) throws IOException {
        Video video = videos.get(id);

        if (video == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        videoFileManager.copyVideoData(video, response.getOutputStream());
    }

    public Video save(Video entity) {
        checkAndSetId(entity);
        entity.setDataUrl(getDataUrl(entity.getId()));
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if (entity.getId() == 0) {
            entity.setId(currentId.incrementAndGet());
        }
    }

    private String getDataUrl(long videoId) {
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        String base = "http://" + request.getServerName() + (((request.getServerPort()) != 80) ?
                ":" + request.getServerPort() : "" );
        return base;
    }
}
