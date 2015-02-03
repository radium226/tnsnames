package radium.parboiled;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressNode;

public class AbstractParser<T> extends BaseParser<T> {

	@SuppressNode
	public Rule EmptyLine() {
		return Sequence(WhiteSpaces(), EndOfLine()).suppressNode();
	}
	
	@SuppressNode
	public Rule WhiteSpace() {
		return AnyOf("\t ");
	}

	@SuppressNode
	public Rule WhiteSpaces() {
		return ZeroOrMore(WhiteSpace());
	}
	
	@SuppressNode
	public Rule EndOfLine() {
		return String("\r\n");
	}
	
	@SuppressNode
	public Rule AnythingButEndOfLine() {
		return NoneOf("\r\n");
	}
	
	@SuppressNode
	public Rule WhiteSpaces(Rule rule) {
		return Sequence(WhiteSpaces(), rule, WhiteSpaces());
	}
	
	@SuppressNode
	public Rule WhiteSpaceOrEndOfLine() {
		return FirstOf(WhiteSpace(), EndOfLine());
	}
	
	@SuppressNode
	public Rule WhiteSpacesOrEndOfLines() {
		return ZeroOrMore(WhiteSpaceOrEndOfLine());
	}
	
	@SuppressNode
	public Rule WhiteSpacesOrEndOfLines(String string) {
		return WhiteSpacesOrEndOfLines(String(string));
	}
	
	public Rule WhiteSpacesOrEndOfLines(Rule rule) {
		return Sequence(WhiteSpacesOrEndOfLines().suppressNode(), rule, WhiteSpacesOrEndOfLines().suppressNode()).skipNode();
	}
	
	@SuppressNode
	public Rule AlphaNumeric() {
		return FirstOf(Letter(), Digit());
	}

	@SuppressNode
	public Rule Letter() {
		return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'));
	}

	@SuppressNode
	public Rule Digit() {
		return CharRange('0', '9');
	}
	
}
