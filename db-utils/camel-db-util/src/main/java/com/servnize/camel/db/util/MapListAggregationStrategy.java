package com.servnize.camel.db.util;

import java.util.ArrayList;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class MapListAggregationStrategy implements AggregationStrategy {

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

		Map<?,?> newBody = newExchange.getIn().getBody(Map.class);
		ArrayList<Map> list = null;
		
		if (oldExchange == null) {
			list = new ArrayList<>();
			list.add(newBody);
			newExchange.getIn().setBody(list);
			return newExchange;
		} else {
			list = oldExchange.getIn().getBody(ArrayList.class);
			list.add(newBody);
			return oldExchange;
		}
	}

}
