var fs = require('fs');
var fse = require('fs-extra');
var marked = require('marked');
var async = require('async');


var docsOutPath = '../../docs/';
var gpsLoggerFaqsPath = '../text/faq/';

getFaqMdsList();

var renderer = new marked.Renderer();
marked.options({
  renderer: renderer
})

renderer.heading = function (text, level) {
  var escapedText = text.toLowerCase().replace(/[^a-zA-Z]+/g, '');
  if(level <= 3){
      return `<h${level}><a name="${escapedText}" href="#${escapedText}">${text}</a></h${level}>`;
  } 
  else {
    return '<h' + level + '>' + text + '</h' + level + '>' + '\r\n\r\n';
  } 
}



function getTop(title, includeIntro){
    var top = fs.readFileSync('scaffolding/top.html', 'utf8');
    if(includeIntro){
      introHeader = fs.readFileSync('scaffolding/intro-header.html', 'utf8');
    }
    else {
      introHeader = "";
    }

    top = top.replace('###INTROHEADER###', introHeader);

    if(!title){
      title = ""
    } 
    else {
      title = title + " - ";
    }
    top = top.replace('###TITLE###', title);
    return top;
} 

function getBottom(includePics){
    var bottom = fs.readFileSync('scaffolding/bottom.html', 'utf8');
    bottom = bottom.replace('###YEAR###',new Date().getFullYear());


    if(includePics){
      footerPics = fs.readFileSync('scaffolding/footer-pics.html', 'utf8');
    } 
    else {
      footerPics = ""
    }

    bottom = bottom.replace('###FOOTERPICS###', footerPics);
    return bottom;

}

function getTitleFromContents(markdown){
  var firstPosition = markdown.indexOf('#');
  var endPosition = markdown.indexOf('\n');
  return markdown.substring(firstPosition, endPosition).replace('#','').trim();
}

function getFaqMdsList(){
  var mdsList = fs.readdirSync(gpsLoggerFaqsPath);
  mdsList = mdsList.filter(function(val){
    return val.endsWith('.md')
  }).map(function(val){
    return gpsLoggerFaqsPath + val;
  });
  
  return mdsList;
}

function getMainPageFiles(){
  var allFiles = JSON.parse(fs.readFileSync('filesToProcess.json'));
  //Inserts FAQ MDs into middle of main page MDs
  allFiles.mainpage = allFiles.mainpage.slice(0,6).concat(getFaqMdsList()).concat(allFiles.mainpage.slice(6));
  return allFiles.mainpage;
}

function getStandaloneFiles(){
  var allFiles = JSON.parse(fs.readFileSync('filesToProcess.json'));
  return allFiles.standalone;
}

function renderMainPage(callback){
  
  var mdsToRender = getMainPageFiles();
  var outFile = fs.createWriteStream(docsOutPath + 'index.html')
  outFile.on('error', function(err){console.log(err)});
  outFile.write(getTop(false,true));

  async.eachSeries(mdsToRender,
    function(filename, cb) {
        fs.readFile(filename, 'utf8', function(err, content) {
          console.log('--------------------------------------------------------\r\n Processing ' + filename + '\r\n--------------------------------------------------------\r\n')

          if (!err) {
            console.log(marked.parse(content));
            outFile.write("<section><div class='lead'>")
            outFile.write( marked.parse(content)+'\r\n');
            outFile.write("</div></section>\r\n\r\n");
          }

          cb(err);
        });
    },
    function(err, results){
      //Footer
      outFile.write(getBottom(true));
      outFile.end();
      callback();
    }
  );
}

function renderFullPages(callback){
  var fullPagesToRender = getStandaloneFiles();

  async.eachSeries(fullPagesToRender, 
    function(fullPage,cb){
        fs.readFile(fullPage.src, 'utf8', function(err,content){
             if (!err) {
                console.log('--------------------------------------------------------\r\n Processing ' + fullPage.src + '\r\n--------------------------------------------------------\r\n')
                var outFile = fs.createWriteStream(docsOutPath + fullPage.out);
                outFile.on('error', function(err){console.log(err)});

                outFile.write(getTop(getTitleFromContents(content), false));
                outFile.write("<section><div class='lead'>")
                console.log(marked.parse(content));
                outFile.write(marked.parse(content)+'\r\n')
                outFile.write("</div></section>\r\n\r\n");
                outFile.write(getBottom(false));
                outFile.end();
             }

             cb(err);
      });
    },
      function(err, results){
      callback();
    }
  );
}

function copyStaticToOutput(callback) {
    fse.copy('static', docsOutPath, function(err){
        if (err) {
            return console.error(err);
        }
        callback();
    });
}

function copyContentImagesToOutput(callback) {
    fse.copy('content/images', docsOutPath + 'images', function (err) {
    if (err) {
      return console.error(err);
    }
    callback();
  });
}

function copyFaqImagesToOutput(callback){
    fse.copy('../text/faq/images', docsOutPath+'images', function(err){
        if (err) {
          return console.error(err);
        }
      callback();
  });
}


async.series([copyStaticToOutput, copyContentImagesToOutput, copyFaqImagesToOutput, renderMainPage, renderFullPages]);


