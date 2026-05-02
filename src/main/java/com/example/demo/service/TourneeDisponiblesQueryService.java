package com.example.demo.service;

import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Requête paginée des tournées « disponibles » (sans transporteur) avec filtre année et recherche,
 * sans charger tout l’historique en mémoire.
 */
@Service
public class TourneeDisponiblesQueryService {

    @Autowired
    private MongoOperations mongoOperations;

    public Page<Tournee> findDisponiblesAssignation(
            List<StatutTournee> statuts,
            List<String> vergerIdsOrNull,
            Integer yearOrNull,
            String textSearch,
            Pageable pageable
    ) {
        List<Criteria> andParts = new ArrayList<>();
        andParts.add(Criteria.where("statut").in(statuts));
// Match documents where transporteur field doesn't exist OR is null
        andParts.add(new Criteria().orOperator(
                Criteria.where("transporteur").is(null),
                Criteria.where("transporteur").exists(false)
        ));
        if (yearOrNull != null && yearOrNull > 0) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.clear();
            cal.set(Calendar.YEAR, yearOrNull);
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date start = cal.getTime();
            cal.add(Calendar.YEAR, 1);
            Date end = cal.getTime();
            andParts.add(Criteria.where("dateDebut").gte(start).lt(end));
        }

        if (vergerIdsOrNull != null && !vergerIdsOrNull.isEmpty()) {
            // Convert to ObjectIds - verger is stored as direct ObjectId in MongoDB
            List<org.bson.types.ObjectId> oids = vergerIdsOrNull.stream()
                    .map(org.bson.types.ObjectId::new)
                    .collect(Collectors.toList());
            andParts.add(Criteria.where("verger").in(oids));
        }

        if (StringUtils.hasText(textSearch)) {
            String q = textSearch.trim();
            Pattern rx = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
            andParts.add(new Criteria().orOperator(
                    Criteria.where("code").regex(rx),
                    Criteria.where("livraisonDestinationNom").regex(rx),

                    Criteria.where("livraisonDestinationAdresse").regex(rx),
                    Criteria.where("observations").regex(rx)
            ));
        }

        Criteria all = new Criteria().andOperator(andParts.toArray(new Criteria[0]));
        Query query = new Query(all).with(pageable);
        List<Tournee> list = mongoOperations.find(query, Tournee.class);
        long total = mongoOperations.count(Query.query(all), Tournee.class);
        return new PageImpl<>(list, pageable, total);
    }}
