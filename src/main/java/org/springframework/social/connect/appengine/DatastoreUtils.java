package org.springframework.social.connect.appengine;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatastoreUtils {
	
	public static <T> List<T> queryForList(PreparedQuery pq, EntityMapper<T> entityMapper) {
		return queryForList(pq, FetchOptions.Builder.withDefaults(), entityMapper);
	}
	
	public static <T> List<T> queryForList(PreparedQuery pq, FetchOptions fetchOptions, EntityMapper<T> entityMapper) {
		List<T> resultList = new ArrayList<T>();		
		for (Entity entity : pq.asIterable(fetchOptions)) {
			resultList.add(entityMapper.mapEntity(entity));
		}
		return resultList;
	}
	
	public static <T> Map<Key, T> queryForMap(PreparedQuery pq, EntityMapper<T> entityMapper) {
		Map<Key, T> resultMap = new HashMap<Key, T>();
		for (Entity entity : pq.asIterable()) {
			resultMap.put(entity.getKey(), entityMapper.mapEntity(entity));
		}
		return resultMap;
	}

    public static interface EntityMapper<T> {
		/** Implementations must implement this method to map each {@code Entity} */
		T mapEntity(Entity entity);
	}

	
}
