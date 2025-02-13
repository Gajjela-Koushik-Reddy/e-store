package com.estore.product_service.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.estore.product_service.config.MinioConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.SnowballObject;
import io.minio.UploadSnowballObjectsArgs;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class ImageServiceImpl {

    @Autowired
    private MinioConfig minioConfig;

    @Value("${minio.bucket}")
    private String bucket;

    private List<String> uploadFilesToMinio(MultipartFile[] files)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException {

        if (files == null)
            throw new NullPointerException("files cannot be null");
        if (files.length == 0)
            throw new RuntimeException("Cannot Upload Empty Files");

        List<String> imageuuids = new ArrayList<>();

        try {
            boolean found = minioConfig.minioClient().bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioConfig.minioClient().makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            // bucket exists
            // upload all the files
            List<SnowballObject> objects = new ArrayList<>();
            for (MultipartFile image : files) {
                if (image.getContentType().isEmpty() || !image.getContentType().startsWith("image/"))
                    throw new RuntimeException("not an valid image file");
                String filename = UUID.randomUUID() + image.getOriginalFilename();
                imageuuids.add(filename);
                objects.add(new SnowballObject(filename,
                        new ByteArrayInputStream(image.getBytes()), image.getSize(), null));
            }
            minioConfig.minioClient().uploadSnowballObjects(
                    UploadSnowballObjectsArgs.builder().bucket(bucket).objects(objects).build());

            return imageuuids;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<String> uploadImages(MultipartFile[] images)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException {

        return uploadFilesToMinio(images);
    }

    public byte[] downloadImage(String imageuuid) {
        try {
            InputStream stream = minioConfig.minioClient()
                    .getObject(GetObjectArgs.builder().bucket(bucket).object(imageuuid).build());
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Image cannot be downloaded" + e.getMessage());
        }
    }

}