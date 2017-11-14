package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class MaxFieldLength {
	final List<Integer> maxLengths;
	final List<BiFunction<Object, Integer, String>> fieldResolvers;
	public MaxFieldLength(List<BiFunction<Object, Integer, String>> fieldResolvers) {
		this.fieldResolvers = fieldResolvers;
		maxLengths = new ArrayList<>(fieldResolvers.size());
		for (int index = 0; index < fieldResolvers.size(); index++) {
			maxLengths.add(0);
		}
	}
	
	public void checkLength(Object item) {
		for (int index = 0; index < fieldResolvers.size(); index++) {
			String fieldContent = (String) fieldResolvers.get(index).apply(item, 0);
			if (maxLengths.get(index) < fieldContent.length()) maxLengths.set(index, fieldContent.length());
		}
	}
	
	public int getMaxLength(int fieldNum) {
		return maxLengths.get(fieldNum);
	}
}
