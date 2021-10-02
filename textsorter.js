//var BASE_URL_NO_PORT = "http://netgear.rohidekar.com";
var BASE_URL_NO_PORT = "http://localhost";
var BASE_URL = BASE_URL_NO_PORT + ":4455";


var _limit;

// Not currently being used even though it's useful
var _loadedTextLength;

// TODO: bad: global state
var _headingNames = [];

// I forgot why we need this. One of the libraries needs dollar for something
// else, but I don't remember which one
var jq = jQuery;

jq.noConflict();

jq(document).ready(function() {

	updateURLIfEmpty(jq.url().param('limit'));
	self._limit = jq.url().param('limit');
	
	loadFile(getUrl(jq("#dir").val(), jq.url().param('filePath')));

});


function getUrl(dir, filePathInListbox) {
	var url = "";
	if (window.location.search.length == 0) {
		url = dir;
	} else {
		url = decodeURIComponent(filePathInListbox);
	}
	return url;
}

function updateURLIfEmpty(limit2) {
	if (limit2 == null) {
		limit2 = 200;
		window.history.pushState("object or string", "Title", document.URL + "&limit=" + limit2);
	}
}

function loadFile(docUrl){

	{
		jq("#dir").val(docUrl);

		window.history.pushState(null, "vi", "index.html?filePath="
				+ encodeURIComponent(docUrl) + "&limit=" + _limit);
	}

	jq.getJSON(
			BASE_URL + "/helloworld/json?filePath=" + encodeURIComponent(docUrl))
		.done(function(result) {
			printTreeAsHtml(result);			
		})
		.fail(function() {
			alert('Failed. Is the server running?');
		});
}

function printTreeAsHtml(response) {
	var level = 0;
	jq("#snippets").val();
	var tree = response.tree;	
	tree.heading = "-root-";
	var text = "";
	printedSnippetCount = 0;
	var headings = {
		0 : {},
		1 : {},
		2 : {},
		3 : {},
		4 : {},
		5 : {},
		6 : {},
		7 : {},
	};
	for (var i = 0; i < tree.length; i++) {
		//var snippet = tree[i];
		getHeadingLevelToHeadings(tree[i], 0, headings);
	}
	for (var i = 0; i < tree.length; i++) {
		var snippet = tree[i];
		text += printSnippetAndSubsnippets(snippet, level + 1, tree, headings, tree);
	}
	jq("#snippets").append(headings + text);
}

var printedSnippetCount = 0;

var colors = ['red','white','SteelBlue','MistyRose','red'];

function printSnippetAndSubsnippets(iSnippet, level, rootTree, headings, parentSnippet) {

	++printedSnippetCount;

	// TODO: bad - global variable
	if (printedSnippetCount > _limit) {
		return "";
	}

	var rText = "<blockquote><div id="
			+ iSnippet.id
			+ " class='item' style='border-width : \"15px\"; background-color: "
			+ colors[level] + "'>";
	rText += printSnippetTree(iSnippet, level, headings, parentSnippet);
	rText += "</div></blockquote>";
	
	for (var j = 4; j > 0; j--) {
		var numberOfNewlines = j-level;
		for (var k = numberOfNewlines; k > 0; k--) {
			rText += "<br>";
		}
	}
	return rText;
}

function printSnippetTree(iSnippet, level, headings, parentSnippet) {
	var snippetTreeHtml = printSnippetHeader(iSnippet, level, headings,
			parentSnippet);
	
	jq.each(iSnippet.subsections, function(i, item) {
		snippetTreeHtml += printSnippetAndSubsnippets(item, level + 1, null, headings, iSnippet);
	});
	return snippetTreeHtml;
}

function printSnippetHeader(iSnippet, level, headings, parentSnippet) {
	var snippetHeaderHtml = "<table><tr><td>"; 
	snippetHeaderHtml += iSnippet.heading + "<br>(" 
		+ iSnippet.id
		+"<br><span style='background-color : pink'>"
		+parentSnippet.heading+"</span>)"
		+"<br>";
	var forbiddenLevel = 1;

	if (level > forbiddenLevel && level < 5) // Level 4 is also best left alone
	{
		snippetHeaderHtml += createTextAreaFor(iSnippet);
		snippetHeaderHtml += "<br></td><td>";
		snippetHeaderHtml += getButtonsHtml(getCategoriesForSnippetAtLevel(level, headings,
				forbiddenLevel), iSnippet);
	}
	snippetHeaderHtml += "<br></td></tr>";
	snippetHeaderHtml += "</table>";
	return snippetHeaderHtml;
}

function createTextAreaFor(iSnippet) {
	var textareaHtml = "<textarea cols=60 rows=10 style='background-color : "
			+ getTextAreaColor(iSnippet) + "'>";
	textareaHtml += iSnippet.freetext;
	textareaHtml += "</textarea>";
	return textareaHtml;
}

function getButtonsHtml(categories, iSnippet) {
	var buttonPanelHtml = "<table><tr><td>";

	var i = 0;
	jq.each(deDupe(sort(categories)), function(pos, obj) {
		i++;
		var buttonColor ;
		buttonColor = 'none';
		if (new RegExp(".*Trash.*").test(obj.heading)) {
			buttonColor = 'purple';
		} else if (new RegExp(".*prod.*").test(obj.heading)) {
			buttonColor = 'yellow';
		} else if (new RegExp(".*usiness.*").test(obj.heading)) {
			buttonColor = 'green';
		} else if (new RegExp(".*areer.*").test(obj.heading)) {
			buttonColor = 'orange';
		} else if (new RegExp(".*rogramming.*ips.*").test(obj.heading)) {
			buttonColor = 'blue';
		} else if (new RegExp(".*tech.*").test(obj.heading)) {
			buttonColor = 'lightblue';
		} else if (new RegExp(".*travel.*").test(obj.heading)) {
			buttonColor = 'lightgreen';
		}
		if (i %20 == 0) {
			buttonPanelHtml += "</td><td>";
		}
		buttonPanelHtml += "<input type=button value='"
			+ obj.heading.replace('\n','').substring(0,14) 
			+'\' onclick=\x22moveTo(\x27'
			+iSnippet.id+'\x27,\x27'
			+ obj.id 
			+'\x27)\x22 '
			+ ' title="' + obj.heading.replace(new RegExp('=+\s*(.*)\s*===?'),'$1') + '" '
			+' style="background-color : '
			+buttonColor+'"><br>\n';
	});

	buttonPanelHtml += "</td></tr></table>";
	return buttonPanelHtml;
}

function sort(categories) {
	var sortedCategories = [];
	
	jq.each(categories, function(heading, id) {
		var p = {
			id : id,
			heading : heading,
		};
	    sortedCategories.push(p);
	});
	return sortedCategories;
}

function getCategoriesForSnippetAtLevel(level, headings, forbiddenLevel) {
	var categories = [];
	if (level > forbiddenLevel) {
		categories = headings[level-2];
	}
	return categories;
}

function getTextAreaColor(iSnippet) {
	var textareaColor = 'white';

	if (iSnippet.freetext == null) {
		textareaColor = 'orange';
	}
	else if (iSnippet.freetext.trim().length == 0) {
		if (iSnippet.subsections.length == 0) {
			textareaColor = 'purple';
		} else {
			textareaColor = 'red';
		}
	}
	return textareaColor;
}

function deDupe(iSortedCategories) {
	var rDedupedCategories = [];
	var deDupedCategoryHeadings = [];
	var capitalizedCategoryHeadings = [];
	jq.each(iSortedCategories, function(heading, id) {
	    var capitalizedHeading = iSortedCategories[heading].heading.replace(/=+\s+(\w+)\s+=+/, capitalizeHeadingFunction); 
	    if (deDupedCategoryHeadings.indexOf(capitalizedHeading) < 0) {
	    	if (capitalizedCategoryHeadings.indexOf(capitalizedHeading.toUpperCase()) < 0) {
				deDupedCategoryHeadings.push(capitalizedHeading);
				rDedupedCategories.push(iSortedCategories[heading]);
				capitalizedCategoryHeadings.push(capitalizedHeading.toUpperCase());
	    	}
	    }
	});
	rDedupedCategories.sort(function(a,b){
	    if(a.heading.toLowerCase() > b.heading.toLowerCase()){ return 1;}
		if(a.heading.toLowerCase() < b.heading.toLowerCase()){ return -1;}
		  return 0;
	});
	return rDedupedCategories;
}

function capitalizeHeadingFunction(match, p2, offset, totalString) {
	return p2.charAt(0).toUpperCase() + p2.slice(1);
};

function getHeadingLevelToHeadings(subtree, level, headingsAtLevels) {
	
	if (subtree.subsections != null) {	
		if (level > -1) {
			headingsAtLevels[level][subtree.heading] = subtree.id;
		}
		for (var i = 0; i < subtree.subsections.length; i++) {
			getHeadingLevelToHeadings(subtree.subsections[i],level+1,headingsAtLevels);
		}
	}
	
	return headingsAtLevels;
	
}

function persistChanges() {
   var latestTextLength = jq("#myTextArea").val().length;
   
   if (latestTextLength == _loadedTextLength) {
		console.debug('text has not changed length');
		return;
   }

   
   jq.ajax({          
            type:  'POST',
            url:   BASE_URL + '/helloworld/persist',
			 data: { 
			 	filePath: encodeURIComponent(jq("#dir option:selected").val()),
			 	newFileContents : encodeURIComponent(jq("#myTextArea").val()),
			 }
         }).done(function() {
			console.debug("successfully written");
			_loadedTextLength = latestTextLength;
         }).fail(function(){alert("fail");});
}

function moveTo(idToBeMoved, idOfTarget) {
	var filePath = jq("#dir").val();


	jq("#"+idToBeMoved).css('background-color','orange');
	jq("#"+idToBeMoved).addClass('fadeOut');

	// javascript animation is expensive. CSS animation is a lot smoother.
	jq.ajax({          
		type:  'POST',
		url:   BASE_URL + '/helloworld/move?filePath=' +filePath +"&id=" +idToBeMoved+ "&destId=" + idOfTarget,
		 data: { 
			//filePath: encodeURIComponent(jq("#dir option:selected").val()),
			//newFileContents : encodeURIComponent(jq("#myTextArea").val()),
		 }
	}).done(function() {
		jq("#"+idToBeMoved).remove();
		console.debug("successfully written: " + idToBeMoved);
	}).fail(function(){alert("fail");});
}


function clearTextArea() {
	jq("#snippets").text("");
}
