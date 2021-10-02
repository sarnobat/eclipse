var urlBase = "http://netgear.rohidekar.com:44451/cmsfs/";
var limit;
var limitRoot = 200;
var preDirLimit;
var pairs;
var depth;
var WIDTH_250 = 250;

$(function() {
	$( "#tabs" ).tabs();
});

// TODO: This code is nice and simple thanks to the functional refactorings. What would 
// be even better is if we could replace the loops with recursion.
$(document).ready(function(){

	updateURLIfEmpty();

	$.getJSON(urlBase + "/list?dirs=" + encodeURIComponent($("#locations").val()) + "&limit=" + limit + "&depth=" + depth,function(response) {
	        $('#status').append("File list obtained. Creating tags...");
		$('#items').empty();
		addSubdirLinks(response);
		pairs = toPairs(response.itemsRecursive);
		// Sorting
		{
			reRenderCards();
		}
		// Thumbnails
		{
			var html = printDirsRecursive(response.itemsRecursive, response, "thumbnail2", limitRoot);
			$('#thumbnailItems').append(html);
			// TODO: write the actual image urls to the new tab link immediately.
		}
		// Hoist (no buttons)
		{
			var html = "";
			//console.debug(html);
			
			html += pairs.map(rootFolderOnly).reduce(DIR_TO_HTML, "");
			$('#hoist').append(html);
		}
		// Hoist
		{
			var html = "";
			//console.debug(html);
			
			html += pairs.map(rootFolderOnly).reduce(DIR_TO_HTML_WITH_BUTTONS, "");
			$('#hoist_with_buttons').append(html);
		}
		// Markup
		{
			var html = printDirsRecursive(response.itemsRecursive, response, "thumbnail2", limitRoot, createThumbnailItemHtml);
			$('#markup').val(html);
		}
		$('#status').append("Tags created. Loading screenshots...");
		loadImagesSequentially(0);
		$('#status').append("Completed.");
	})
	.fail(function() {
		console.debug( "error" );
		$('#status').append("Error");
	});
	$('#status').append("This might take a while...");
});

function reRender() {
	//pairs = shuffle(pairs);
	//pairs = shuffle(pairs);
	reRenderCards();
	loadImagesSequentially(0);
}

function reRenderCards() {
	$('#itemsNew').empty();
	var html = "";
	html += pairs.map(DIR_TO_HTML_CARDS).join('');
	$('#itemsNew').append(html);
}

// descending
function BY_CREATED(a, b) {

  if (a[Object.keys(a)[0]].created < b[Object.keys(b)[0]].created ) {
    return -1;
  }
  if (a[Object.keys(a)[0]].created > b[Object.keys(b)[0]].created ) {
    return 1;
  }
  // a must be equal to b
  return 0;
}

function shuffle(array) {
  console.debug('broken after ordering by timestamp');
  var currentIndex = array.length, temporaryValue, randomIndex ;

  // While there remain elements to shuffle...
  while (0 !== currentIndex) {

    // Pick a remaining element...
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex -= 1;

    // And swap it with the current element.
    temporaryValue = array[currentIndex];
    array[currentIndex] = array[randomIndex];
    array[randomIndex] = temporaryValue;
  }

  return array;
}

function updateURLIfEmpty() {
	var locations = $.url().param('locations');
	if (locations == null || locations == '') {
			updateURL();
	} else {
		$("#locations").val(decodeURIComponent(locations));
	}

  	limit = $.url().param('limit');
	if (limit == null) {
			limit = 100;
			window.history.pushState("object or string", "Title", document.URL + "&limit=" + limit);
	}
	preDirLimit = limit;
	
	depth = $.url().param('depth');
	if (depth == null) {
			depth = 1;
			window.history.pushState("object or string", "Title", document.URL + "&depth=" + depth);
	}
}

function updateURL() {
	history.pushState(null, null, '/coagulate?locations=' + encodeURIComponent($("#locations").val())); // HTML5
}

function dirToFilesInDirParameterized(dirContents) {
	return function dirToFilesInDir(dir) {
		return { 
			"dir" : dir,
			"filesInDir" : dirContents[dir] ,
			"subdirs" : dirContents[dir].dirs
		};
	};
}

function toPairs(obj) {
	var ret = [];
	var keys = Object.keys(obj).sort();// Sometimes you want to sort, sometimes shuffle. Make this toggleable
	for (var i = 0; i < keys.length; i++) {
		var key = keys[i];
		var val = obj[key];
		var out = {};
		out[key] = val;
		ret.push(out);
	}
	return ret;
}

function DIR_TO_HTML_WITH_BUTTONS(accumulator, dirPair) {
	return dirToHtmlWithButtons(accumulator, dirPair);
}

function dirToHtmlWithButtons(accumulator, dirPair) {

	var key = Object.keys(dirPair)[0];
	var val = dirPair[key];
	
	return accumulator + "<tr><td><h2>" + lastPartOf(key) +"</h2></td></tr>" + dirObjToHtmlWithButtons(val, key);
}

function DIR_TO_HTML(accumulator, dirPair) {
	return dirToHtml(accumulator, dirPair);
}

function dirToHtml(accumulator, dirPair) {

	var key = Object.keys(dirPair)[0];
	var val = dirPair[key];
	
	return accumulator + "<h2>" + lastPartOf(key) +"</h2>" + dirObjToHtml(val, key);
}

function startsWith(str, prefix) {
	return (str.lastIndexOf(prefix,0) == 0);
}

var hoistButtonId = 0;
var level = 1000; // This is just a unique number, not semantically significant
function dirObjToHtmlWithButtons(val, dir) {
	var html = "";
	// recurse pre-order
	if (val.dirs) {
		var dirnames = Object.keys(val.dirs);
		for (var i = 0; i < dirnames.length; i++) {
			var dirname =  dirnames[i];
			if (lastPartOf(dirname).lastIndexOf('_+',0) == 0) {
				var dirObj = val.dirs[dirname];
				html +=  dirObjToHtmlWithButtons(dirObj, dirname);
			}
		}
	}
	var dirName = lastPartOf(dir);
	--level;
	html += "<tr><td><span id='level-" + level +"'>";

	html += "<h3>" + dirName + "</h3>";
	html += "<textarea cols='100' rows='1'>" + dir + "</textarea>";
	html += "<br>";
	var pairs = toPairs(val);
	var files2 = preDirLimit;
	for (var i = 0; i < pairs.length; i++) {
		var pair = pairs[i];
		var filename = Object.keys(pair)[0];
		if (filename == 'dirs') {
			continue;
		}
		var fileObj = pair[filename];
		var id = "hoistwithbuttons-" + filename;
		++hoistButtonId;
		html += "<span id='"+id+"' style='float : left' align='left'>";
		html += "<br>";
		html += createThumbnailItemHtml("", fileObj);
		html += "<br>";
		html += "<br>";
		html += "<br>";
		html += "<button onclick='up(\""+filename +"\",\""+id+"\", "+level+", this)'>+</button>";
		html += "<button onclick='down(\""+filename +"\",\""+id+"\", "+level+", this)'>-</button><br>";
		html += "<button onmousedown='this.style.cssText = \"background-color : peachpuff\";' onclick='moveFile(this, \"" + filename + "\",\"duplicates\",\""+id+"\")' style='background-color:"+getColorForDir(dirName)+"'>dup</button><br>";
		html += "</span>";
		--files2;
		if (files2 < 1 && !startsWith(dirName, '_+')) {
			break;
		}
	}
	html += "</span></td></tr>";
		
	if (dirnames != null) {
		console.debug('dirnames = ' + dirnames + '\nMove declaration to right place');
		for (var i = 0; i < dirnames.length; i++) {
			var dirname =  dirnames[i];
			if (lastPartOf(dirname).lastIndexOf('_-',0) == 0) {
				var dirObj = val.dirs[dirname];
				html += dirObjToHtmlWithButtons(dirObj, dirname);
			}
		}
	}
	return html;
}

function up(field,id, currentLevelId, button) {
	$.getJSON("http://netgear.rohidekar.com:4463/helloworld/moveUp?path=" + encodeURIComponent(field),function(result){
		var targetLevel = currentLevelId + 1;
		// TODO: handle the case where target level doesn't exist
		var elem = $("[id='" + id + "']").remove();
		$("#level-" + targetLevel).append(elem);
		removeImageElement(field);
	});
}

function down(field,id, currentLevelId, button) {
	$.getJSON("http://netgear.rohidekar.com:4463/helloworld/moveDown?path=" + encodeURIComponent(field),function(result){
		var targetLevel = currentLevelId - 1;
		// TODO: handle the case where target level doesn't exist
		var elem = $("[id='" + id + "']").remove();
		$("#level-" + targetLevel).append(elem);
		// TODO: why is this called twice?
		removeImageElement(field);
    });
}

function dirObjToHtml(val, dir) {
	var html = "";
	// recurse pre-order
	if (val.dirs) {
		var dirnames = Object.keys(val.dirs);
		for (var i = 0; i < dirnames.length; i++) {
			var dirname =  dirnames[i];
			if (lastPartOf(dirname).lastIndexOf('_+',0) == 0) {
				var dirObj = val.dirs[dirname];
				html +=  dirObjToHtml(dirObj, dirname);
			}
		}
	}
	var dirName = lastPartOf(dir);
	html += "<h3>" + dirName + "</h3><textarea>" + dir + "</textarea><br>";
	var pairs1 = toPairs(val).filter(function(aPair){aPair[0] != 'dirs'});
	var pairs;
	if (startsWith(dirName, '_+')) {
		pairs = pairs1;
	} else {
		pairs = pairs1.slice(0, preDirLimit);
	}
	// TODO: make the other createThumbnailItemHtml() calls be done via map reduce
	html += pairs.map(pairVal).reduce(createThumbnailItemHtml, "");
	if (dirnames != null) {
		console.debug('dirnames = ' + dirnames + '\nMove declaration to right place');
		for (var i = 0; i < dirnames.length; i++) {
			var dirname =  dirnames[i];
			if (lastPartOf(dirname).lastIndexOf('_-',0) == 0) {
				var dirObj = val.dirs[dirname];
				html += dirObjToHtml(dirObj, dirname);
			}
		}
	}
	return html;
}

function pairVal(pair) {
	return pair[1];
}

// also retains ranking dirs
function rootFolderOnly(dirPair) {
	var ret = jQuery.extend(true, {}, dirPair);
	var key = Object.keys(dirPair)[0];
	// Assumption : the right hand side is deep copied.
	val = ret[key];
	if (val.dirs) {
		var subDirs = Object.keys(val.dirs);
		for (var i = 0; i < subDirs.length; i++) {
			var subDir = subDirs[i];
			var dirName = subDir.substr(subDir.lastIndexOf('/') + 1);
			if (dirName.lastIndexOf('_', 0) == 0) {
				// retain it
			} else {
				delete val.dirs[subDir];
			}
		}
	}
	return ret;
}

function lastPartOf(str) {
	return str.substr(str.lastIndexOf('/') + 1);
}

function filesOnly(anObj) {
	if (anObj.dirs == null) {
		return true;
	}
	return false;
}

// For thumbnails
function printDirsRecursive(dirItems, response, idIn, iLimit, loadImmediate) {
	var html = "";
	var items = dirItems;

	var id = 0;
	// For each directory
	var sortedItems = Object.keys(dirItems).sort();
	// TODO: Move current dir to front
	for(var dirIndex in sortedItems) { 
		var dirPath = sortedItems[dirIndex];
		var categoryName1 = "";
		categoryName1 = dirPath.split('/').pop();
		html += "\n\n\n<h3><a href='http://netgear.rohidekar.com/coagulate/?locations="+encodeURIComponent(dirPath) +"'>\n"+ categoryName1 + "\n</a></h3>\n\n\n";
		var filesInDir = items[dirPath];
		if (filesInDir.length < 1) {
			continue;
		}
		var filesInDirSortedMap = shuffle(Object.keys(filesInDir));
		// For each regular file in the directory
		for (var sortedIndex in filesInDirSortedMap) {
			var filepath = filesInDirSortedMap[sortedIndex];
			if (filepath.match(/.*(nohup.out$|Thumbs.db|[pP]icasa.ini)/)) {
				continue;
			}
			if (filepath == 'dirs') {
			} else {
				++id;
				if (id > iLimit) {
//					continue;
				}
				var height = 80;
				if (categoryName1 == '_-1' || categoryName1 == 'back' || categoryName1 == 'duplicates' || categoryName1 == 'not good' || categoryName1 == 'small') {
					height = 40;
				} else if (categoryName1 == '_+1') {
					height = 140;
				}
				html += createThumbnailItemHtml("", filesInDir[filepath], loadImmediate, height);
				html += "\n\n";
			}
		}
		// For subdirs
		for (var sortedIndex in filesInDirSortedMap) {
			var filepath = filesInDirSortedMap[sortedIndex];
			if (filepath == 'dirs') {
				var subdirItems = filesInDir['dirs'];
				html += "<blockquote>" + printDirsRecursive(subdirItems, response, idIn + "-" + id, limit, loadImmediate) + "</blockquote>";
			} else {
			}
		}
	}
	return html;
}

// reduction function and regular function
// No buttons
function createThumbnailItemHtml(accumulator, fileObj, loadImmediate, size) {	
	
	var fileSystem = fileObj.fileSystem;

	// File types to ignore	
	var res = fileSystem.match(/.*\.((txt)|(lnk))/gi);
	if (res != null) {
		return;
	}

	{
		var toBeAppended = accumulator;
		var httpUrl = fileObj.httpUrl;
		var thumbnailUrl = fileObj.thumbnailUrl;

		var height = size;
		if (size == null) {
			height = 100;
		}
		
		// Images
		{
			var res = fileSystem.match(/.*\.((jpe?g)|(png)|(gif)|(bmp))/gi);
			if (res !=null) {
				toBeAppended += '\t<a href=\'' + httpUrl + '\' target="_blank">\n';
				var degreesToOrient = 0;
				if (fileObj.exif) {
					degreesToOrient = 90*(parseInt(fileObj.exif.orientation) - 1);
				}
				// height ensures buttons don't jump left and right between items for 
				// easier mass sorting. But it's not as nice to view.
				// The server-side thumbnailator reorients the image correctly
				if (loadImmediate) {
					toBeAppended += '\t\t\t<img src="' + encodeURI(httpUrl) + '" onmouseenter="zoom(this)" style="transform: rotate(' + degreesToOrient + 'deg); height : '+height+'px" class="loadImageSequentially" />\n';
				} else {
					toBeAppended += '\t\t\t<img src="http://netgear.rohidekar.com/static/icons/Orb_Icons_001.png" data-src="' + encodeURI(httpUrl + "?width=" + WIDTH_250) + '" onmouseenter="zoom(this)" style="transform: rotate(' + degreesToOrient + 'deg); height : '+height+'px" class="loadImageSequentially thumbnail" />\n';
				}
				//data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
				toBeAppended += '\t</a>\n';
			}
		}
		// Videos
		{
			var matchesVideo = fileSystem.match(/.*((mp4)|(webm)|(mov)|(mkv)|(flv)|(avi)|(mts))/gi);
			if (matchesVideo != null) {
				var simpleName = fileObj.httpUrl.split('/').pop();
				toBeAppended += '<img src=\"' +thumbnailUrl+'\" height="'+height+'px" title="'+ simpleName +'">';
			}
		}
		// PDFs
		{
			var matchesVideo = fileSystem.match(/.*((pdf)|(tif))/gi);
			if (matchesVideo != null) {
				toBeAppended += '<img src=\"' +thumbnailUrl+'\" height="'+height+'px">';
			}
		}
		toBeAppended = "<span id='thumbnail-"+ fileSystem +"'>\n" + toBeAppended;
		toBeAppended += "&nbsp;\n</span>";
	}	
	return toBeAppended;
}

function addSubdirLinks(response) {

	var subdirectories = Object.keys(response.itemsRecursive);
	for(var dirIndex in subdirectories) { 

		var filesInDir = Object.keys(response.itemsRecursive[subdirectories[dirIndex]].dirs);

		if (filesInDir.length < 1) {
			continue;
		}
		for (var sortedIndex in filesInDir) {
			var filepath = filesInDir[sortedIndex];
			$('#subdirs').append("<tr><td><a href='/coagulate/?locations=" +encodeURIComponent(filepath)+ "'>"+filepath+"</a></td></tr>");
		}
	}
}
function loadImagesSequentially(i) {
	var images = $('.loadImageSequentially');
	if (i >= images.length) {
		return
	}
	var datasrc = $(images[i]).attr('data-src');
	$(images[i]).attr('src',datasrc);
	var a = function(j) {
		loadImagesSequentially(++i);
	};
	$(images[i]).load(a(i), function() {
		
		if (!$( this ).hasClass('thumbnail')) {
			// compute the height and width such that this will fit inside a square of fixed size
			if ($( this ).get(0).height >= $( this ).get(0).width) {
				$( this ).get(0).style.height = WIDTH_250 + 'px';
				$( this ).get(0).style.width = 'auto';
			} else 	if ($( this ).get(0).height < $( this ).get(0).width) {
				$( this ).get(0).style.width = WIDTH_250 + 'px';
				$( this ).get(0).style.height = 'auto';
			} else {
				debugger;
			}
		} else {
				$( this ).get(0).style.height = '80px';
				$( this ).get(0).style.width = 'auto';
		}
	});
}

// item = filepath
// key = dirpath
// item2 = fileObj
// 
function createItemHtml(response, item, key, unused, fileObj, subDirs)
{	
	var image = fileObjToHtml(fileObj);
	
	var res1 = fileObj.fileSystem.match(/.*fuse.hidden.*/gi);
	if (res1 !=null) {
		return;
	}
	var res2 = fileObj.fileSystem.match(/.*aufs/gi);
	if (res2 != null) {
		return;
	}
	var width = WIDTH_250 + "px";
	
	var localLink = "<a href=\"file://" + fileObj.fileSystem.toString() +"\">" + "Local Link" + "</a>";

	var idstringUnescaped = "card-" + fileObj.fileSystem;	
	var idstring = idstringUnescaped.replace(/'/g, "&apos;");
	id += 1;
	var toBeAppended =	  "<tr id='" +idstring+"'><td><table><tr><td>"
		+ image + "<td><td>"
		+ createButtonsPanel(item, subDirs, key, idstring)
		+ "</td>"
		+ "</tr>"
		+ "<tr>"
		+ "<td colspan=2>"
		+ prettyDate(new Date(fileObj.created)) + '<br>' 
		+ "<textarea rows=4 style='width:" + width+"; word-wrap:break-word;'>" 
		+ decodeURIComponent(item) + '\n\n' 
//		+ fileObj.created
		+ "</textarea></td>"
		+ "<td><a href='" +fileObj.httpUrl+"'>" + fileObj.httpUrl + "</a><br>" +localLink
		+ "<br><br>"+fileObj.thumbnailUrl + "</td>"
		+ "</tr>"
		+ "</table></td></tr>";

	return toBeAppended;
}

function prettyDate(date) {
    var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    return date.getUTCFullYear() + ' ' +  months[date.getUTCMonth()] + ' ' + date.getUTCDate();
}


// item = filepath
// key = dirpath
function createButtonsPanel(item, subdirs, key, idstring) {

	var buttons = "<table><tr><td>";							
	{
		var escaped = item.replace(/'/g, "&apos;");
		var idstringescaped = idstring.replace(/'/g, "&apos;");
//		if (idstring.indexOf("'") > -1) {
//			console.debug(idstringescaped);
//		}
		buttons += "<button onclick='moveFileToParent(\"" + escaped + "\",\""+idstringescaped+"\")'>Wrong Category</button><br>";
		var subdirsArray = Object.keys(subdirs).sort(sortFunction);
		{
			var buttonsAdded = 0;
			for (var dir in subdirsArray) {
				buttonsAdded++;
				if (buttonsAdded%15 == 0) {
					buttons += "</td><td>";
				}
				var dirPath = subdirsArray[dir];				
				var dirName = dirPath.substr(dirPath.lastIndexOf('/') + 1);
				var res1 = dirName.match(/\..*/gi);
				
				if (res1 != null) {
					continue;
				}
				buttons += "<button onmousedown='this.style.cssText = \"background-color : orange\";' onclick='moveFile(this, \"" + escaped + "\",\"" + dirName + "\",\""+idstringescaped+"\")' style='background-color:"+getColorForDir(dirName)+"'>"+dirName+"</button><br>";
			}
		}
	}
	buttons += "<br>";
	buttons += "<button onclick='moveToNonExistingFolder(this,\"" +escaped+"\",\""+idstringescaped+"\")'>MOVE to custom</button><br>";
	buttons += "<br>";
	buttons += "<button onclick='copyFileToFolder(\"" + escaped + "\", \"/media/sarnobat/e/Drive J/pictures/Other (new)/pictures/misc_sync_master/favorites\",\""+idstringescaped+"\", this)'>COPY TO favorites</button><br>";
	buttons += "</td></tr></table>";
	return buttons;
}

// image
function fileObjToHtml(fileObj) {
	// height ensures buttons don't jump left and right between items for easier mass sorting. But it's not as nice to view
	var item = fileObj.fileSystem;
	{
		var res = item.match(/.*\.((txt)|(lnk))/gi);
		if (res !=null) {
			return;
		}
	}
	
	{
		var toBeAppended = "";
		var httpUrl = fileObj.httpUrl;
		var thumbnailUrl = fileObj.thumbnailUrl;
		{
			{
				var res = item.match(/.*\.((jpe?g)|(png)|(gif)|(bmp))/gi);
				if (res !=null) {
					toBeAppended += '<a href=\'' + httpUrl + '\' target="_blank">';
					var degreesToOrient = 0;
					if (fileObj.exif) {
						degreesToOrient = 90*(parseInt(fileObj.exif.orientation) - 1);
					}
					toBeAppended += '<img src="http://www.htmlgoodies.com/images/1x1.gif" width="' + WIDTH_250 + 'px" height="1px"><br>';
					toBeAppended += '<img class="loadImageSequentially" src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" data-src="' + encodeURI(httpUrl +'?width=' + WIDTH_250) + '" style="transform: rotate(' + degreesToOrient + 'deg)"/>';
					toBeAppended += '</a>';
				}
			}
		}
		{
			var matchesVideo = item.match(/.*((mp4)|(webm)|(mov)|(mkv)|(flv)|(avi)|(mts))/gi);
			if (matchesVideo != null) {
				toBeAppended += '<img src=\"' +thumbnailUrl+'\">';
			}
		}
		{
			var matchesVideo = item.match(/.*((pdf)|(tif))/gi);
			if (matchesVideo != null) {
				toBeAppended += '<img src=\"' +thumbnailUrl+'\" width=200>';
			}
		}
	}	
	return toBeAppended;
}

function DIR_TO_HTML_CARDS(dirObj) {
	return dirToHtmlCards(dirObj);
}

function dirToHtmlCards(dirObj) {
	var dir = Object.keys(dirObj);
	var files = toPairs(dirObj[dir]).slice(0, limitRoot).filter(filesOnly).sort(BY_CREATED).reverse();
	var subdirs = dirObj[dir].dirs;
	var html = "";
	html += files.map(fileToHtmlParameterized(subdirs)).join('');
	return html;
}

var id = 0;

function fileToHtmlParameterized(subdirs) {
	return function fileToHtml(filePair) {
		var ret = "";
		var filePath = Object.keys(filePair)[0];
		var fileObj = filePair[filePath];
	
	
		if (fileObj.fileSystem.match(/.*(img|nohup.out$|Thumbs.db|[pP]icasa.ini)/)) {
			ret += "";
		} else {
			var dirPath = filePath.substring(0, filePath.lastIndexOf("/"));
			ret += createItemHtml(null, filePath, dirPath, null, fileObj, subdirs);
		}
		return ret;
	};
}

function moveToNonExistingFolder(elem, fileToMove, idstring) {
	var newCategorySimpleName = prompt("Category name:");
	moveFile(elem, fileToMove, newCategorySimpleName, idstring);
}

function moveFile(button, filePath, destinationDirSimpleName,id) {
	$.getJSON(urlBase + "/move?filePath="+encodeURIComponent(filePath) + "&destinationDirSimpleName=" + encodeURIComponent(destinationDirSimpleName),function(response){
		removeImageElement(filePath);
		// TODO: Also remove from pairs global variable so it doesn't keep coming back when we shuffle
	});
}

function removeImageElement(filePath) {
		//var next = $("[id='card-" + encodeURIComponent(filePath) + "']").get(0);
		$("[id='card-" + filePath + "']").remove();
		$("[id='thumbnail-" + filePath + "']").remove();
		$("[id='hoistwithbuttons-" + filePath + "']").remove();
		//if (next == null) {
		//	debugger;
		//}
		//next.scrollIntoView();
}

function sortFunction(a, b) {
	if (a.toLowerCase() < b.toLowerCase()) return -1;
	if (a.toLowerCase() > b.toLowerCase()) return 1;
	return 0;
}
							
function getColorForDir(dirName) {								
	var color = "";
	if (dirName == 'other' || dirName == 'divas') {
		color = 'pink';
	} else if (dirName.match(/Atletico/i) || dirName == 'brst') {
		color = 'red';
	}
	else if (dirName.match(/Liverpool/i)) {
		color = 'red';
	}
	 else if (dirName == 'legs') {
		color = 'yellow';
	}  else if (dirName == 'navel') {
		color = 'lightpurple';
	}  else if (dirName == 'teen') {
		color = 'lavender';
	} else if (dirName.match(/soccer/i)) {
		color = 'green';
	} else if (dirName == 'ind') {
		color = 'orange';
	}
	return color;
}

function addslashes( str ) {
    return (str + '').replace(/[\\"']/g, '\\$&').replace(/\u0000/g, '\\0');
}

function moveFileToParent(filePath, id) {
	$.getJSON(urlBase + "/moveToParent?filePath="+encodeURIComponent(filePath), function(response){
		removeImageElement(filePath);
		//$("#" + id).remove();
	});
}

function copyFileToFolder(filePath, destinationDirPath, id, button) {
	$.getJSON(urlBase + "/copyToFolder?filePath="+encodeURIComponent(filePath) + "&destinationDirPath=" + encodeURIComponent(destinationDirPath), function(response){
		$(button).css('background-color','green');
		$("#" + id).css('color','white');
	});
}

function shuffle(array) {
  var currentIndex = array.length, temporaryValue, randomIndex ;

  // While there remain elements to shuffle...
  while (0 !== currentIndex) {

    // Pick a remaining element...
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex -= 1;

    // And swap it with the current element.
    temporaryValue = array[currentIndex];
    array[currentIndex] = array[randomIndex];
    array[randomIndex] = temporaryValue;
  }

  return array;
}

function openHtmlTextInNewTab() {
	var newWin = open('url', '_blank', '');
	newWin.document.write($("#thumbnailItems").html());
	newWin.document.title = document.title;
}

