package dakara.eclipse.plugin.kavi.picklist;

import java.util.List;
import java.util.function.BiFunction;

public class FieldCollectorTransform {
	private MaxFieldLength maxFieldLength;
	List<BiFunction<Object, Integer, String>> fieldResolvers;
	final List<Object> items;
	
	public FieldCollectorTransform(List<BiFunction<Object, Integer, String>> fieldResolvers, List<Object> items) {
		this.fieldResolvers = fieldResolvers;
		this.maxFieldLength = new MaxFieldLength(fieldResolvers);
		this.items = items;
		for (Object item: items) {
			maxFieldLength.checkLength(item);
		}
	}
	
	public String asAlignedColumns() {
		StringBuilder output = new StringBuilder();
		for (Object item : items) {
			output.append(alignFields(item));
			output.append('\n');
		}
		return output.toString();
	}
	
	private String alignFields(Object item) {
		StringBuilder output = new StringBuilder();
		int fieldNum = 0;
		for (BiFunction<Object, Integer, String> resolver : fieldResolvers) {
			String fieldContent = (String) resolver.apply(item, 0);
			int padLength = maxFieldLength.getMaxLength(fieldNum++) - fieldContent.length();
			padLength++;
			output.append(fieldContent);
			padBuffer(output, ' ', padLength);
		}
		return output.toString();
	}
	
	private void padBuffer(StringBuilder buffer, char padCharacter, int count) {
		for (int index = 0; index < count; index++) {
			buffer.append(padCharacter);
		}
	}
}
