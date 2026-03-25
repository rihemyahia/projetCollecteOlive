package com.example.demo.repository;

import com.example.demo.model.Verger;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VergerRepository extends MongoRepository<Verger, String> {
}