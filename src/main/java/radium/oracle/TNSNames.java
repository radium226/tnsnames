package radium.oracle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

import radium.parboiled.AbstractParser;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class TNSNames {

	@BuildParseTree 
	static class Parser extends AbstractParser<Object> {

		Rule TNSNames() {
			return Sequence(push(Lists.newArrayList()), ZeroOrMore(FirstOf(EmptyLine(), Comment(), Sequence(Entry(), peekAndAdd(pop()))).skipNode()).skipNode(), EOI);
		}
		
		Rule Comment() {
			return Sequence("#", ZeroOrMore(AnythingButEndOfLine()), EndOfLine()).suppressSubnodes();
		}
		
		Rule Entry() {
			return Sequence(WhiteSpacesOrEndOfLines(NetServiceNames()), WhiteSpacesOrEndOfLines("=").suppressNode(), WhiteSpacesOrEndOfLines(Parameter()), popAndPopAndPushEntry());
		}
		
		Rule NetServiceNames() {
			return Sequence(push(Lists.newArrayList()), WhiteSpacesOrEndOfLines(NetServiceName()), ZeroOrMore(Sequence(WhiteSpacesOrEndOfLines(",").suppressNode(), WhiteSpacesOrEndOfLines(NetServiceName())).skipNode()).skipNode()).skipNode();
		}
		
		@SuppressWarnings("unchecked")
		boolean peekAndAdd(Object value) {
			return ((List<Object>) peek()).add(value);
		}
		
		Rule NetServiceName() {
			return Sequence(AlphaNumericsOrUnderScoresOrDots().suppressSubnodes(), peekAndAdd(match())).skipNode();
		}
		
		Rule AlphaNumericsOrUnderScoresOrDots() {
			return OneOrMore(FirstOf(AlphaNumeric(), ".", "_"));
		}
		
		@SuppressSubnodes
		Rule Key() {
			return Sequence(AlphaNumericsOrUnderScoresOrDots(), push(match())).skipNode();
		}
		
		Rule Value() {
			return Sequence(push(Lists.newArrayList()), FirstOf(WhiteSpacesOrEndOfLines(Sequence(OneOrMore(FirstOf(AlphaNumeric(), ".", "-", "_")).suppressNode(), peekAndAdd(match())).suppressNode()), OneOrMore(WhiteSpacesOrEndOfLines(Sequence(Parameter(), popAndPeekAndAdd()))).skipNode()).skipNode());
		}
		
		Rule Parameter() {
			return Sequence(WhiteSpacesOrEndOfLines("(").suppressNode(), WhiteSpacesOrEndOfLines(Key()).skipNode(), WhiteSpacesOrEndOfLines("="), WhiteSpacesOrEndOfLines(Value()), popAndPopAndPushParameter(), WhiteSpacesOrEndOfLines(")").suppressNode());
		}
		
		boolean popAndPeekAndAdd() {
			Object value = pop();
			return peekAndAdd(value);
		}
		
		boolean popAndPopAndPushParameter() {
			Object values = pop();
			Object name = pop();
			return push(new TNSNames.Parameter((String) name, (List<Object>) values));
		}
		
		boolean popAndPopAndPushEntry() {
			Object descriptionParameter = pop();
			Object services = pop();
			return push(new TNSNames.Entry((List<String>) services, (TNSNames.Parameter) descriptionParameter));
		}
		
	}
	
	public static class Parameter {
		
		private String name;
		private List<Object> values;
		
		public Parameter(String name, List<Object> values) {
			super();
			this.name = name;
			this.values = values;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(Parameter.class)
					.add("name", this.name)
					.add("values", this.values)
				.toString();
		}
		
		private void writeTo(PrintStream printStream, int indent, boolean indentFirst) {
			printStream.print((indentFirst ? Strings.repeat("  ", indent) : "") + "(" + this.name + "=");
			for (Object value : values) {
				if (value instanceof String) {
					String valueAsString = (String) value;
					printStream.print(valueAsString);
				} else if (value instanceof Parameter) {
					Parameter valueAsParameter = (Parameter) value;
					printStream.println();
					valueAsParameter.writeTo(printStream, indent + 1, true);
				} else {
					throw new IllegalStateException();
				}
			}
			printStream.print(")");
		}
		
	}
	
	public static class Entry {
		
		private List<String> services;
		private Parameter parameter;
		
		public Entry(List<String> services, Parameter parameter) {
			super();
			this.services = services;
			this.parameter = parameter;
		}
		
		public List<String> getServices() {
			return services;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(Entry.class)
					.add("services", this.services)
					.add("parameter", this.parameter)
				.toString();
		}
		
		private void writeTo(PrintStream printStream, int indent) {
			printStream.print(Joiner.on(",").join(services) + "=");
			parameter.writeTo(printStream, indent + 1, false);
			printStream.println();
		}
		
		public Parameter getParameter() {
			return parameter;
		}
		
	}
	
	final public static String TNSNAMES_FILE_NAME = "tnsnames.ora";
	
	private List<Entry> entries;
	
	private TNSNames(List<Entry> entries) {
		super();
		
		this.entries = entries;
	}
	
	public static TNSNames readFrom(File file, Charset charset) throws IOException {
		String text = Files.toString(file, charset);
		return readFrom(text);
	}
	
	public static TNSNames readFrom(File file) throws IOException {
		return readFrom(file, Charsets.UTF_8);
	}
	
	public static TNSNames readFrom(String text) {
		Parser parser = Parboiled.createParser(Parser.class);
		ParsingResult<?> parsingResult = new BasicParseRunner<Object>(parser.TNSNames()).run(text);
		return new TNSNames((List<Entry>) parsingResult.resultValue);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(TNSNames.class)
				.add("entries", this.entries)
			.toString();
	}
	
	public void writeTo(PrintStream printStream) {
		for (Entry entry : entries) {
			entry.writeTo(printStream, 0);
			printStream.println();
		}
	}
	
	public void writeTo(File file) throws IOException {
		PrintStream printStream = new PrintStream(file);
		writeTo(printStream);
		printStream.close();
	}
	
	public TNSNames expandServices() {
		List<Entry> expandedEntries = Lists.newArrayList();
		for (Entry entry : entries) {
			Parameter parameter = entry.getParameter();
			for (String service : entry.getServices()) {
				Entry expandedEntry = new Entry(Arrays.asList(service), parameter);
				expandedEntries.add(expandedEntry);
			}
		}
		return new TNSNames(expandedEntries);
	}
	
	public static String expandServices(String folder) throws IOException {
		File temporaryDirectory = Files.createTempDir();
		readFrom(new File(folder, TNSNAMES_FILE_NAME)).expandServices().writeTo(new File(temporaryDirectory, TNSNAMES_FILE_NAME));
		return temporaryDirectory.getPath();
	}
	
}
