// 필요 작성 기능
// 1. summernote 이미지 업로드 ------ http://devofhwb.tistory.com/67
//     ------   음....... 꽤 복잡해질듯. 사진 여러개 처리해야됨
//     ------             블로그 소스방식은 게시판이 만들어지기 전에 이미지를 업로드 하게 됨.. 취소하면?
// 2.

var photo_id = [];

$(document).ready(function() {
	$('#summernote').summernote({
		height : 250,
		minHeight: 250,             // set minimum height of editor
		maxHeight: null,             // set maximum height of editor
		// 이 함수가 실행되는 시점이 최종 submit일때면 일이 쉬워짐
		callbacks: {
	          onImageUpload: function(files, editor, welEditable) {
	            for (var i = files.length - 1; i >= 0; i--) {
	              sendFile(files[i], this);
	            }
	          },
	          onMediaDelete : function(files, editor, $editable) {
	              console.log(files[0].src); // img 

	             // remove element in editor 
	              deleteFile(files[0],this);
	              files.remove();
	             
	          }
	        }
		,
	    lang : 'ko-KR'
	});
});

function sendFile(file, el) {
    var form_data = new FormData();
    form_data.append('file', file);
    $.ajax({
      data: form_data,
      type: "POST",
      url: '/BLAuction/photoupload.bla',
      cache: false,
      contentType: false,
      enctype: 'multipart/form-data',
      processData: false,
      success: function(data) {
    	  console.log(data);
    	  photo_id.push(data.photo_id);
    	  var url = data.photo_path+'\\'+data.photo_name;
        $(el).summernote('editor.insertImage', url);
        $('#imageBoard > ul').append('<li><img src="'+url+'" width="480" height="auto"/></li>');
      },
      error: function(data){
    	  alert(data);
      }
    });
  }

function deleteFile(file, el) {
    var form_data = new FormData();
    form_data.append('deletefile', file);
    $.ajax({
      data: form_data,
      type: "POST",
      url: '/BLAuction/photoDelete.bla',
      cache: false,
      contentType: false,
      enctype: 'multipart/form-data',
      processData: false,
      success: function(data) {
    	  console.log(data);/*
    	  photo_id.push(data.photo_id);
    	  var url = data.photo_path+'\\'+data.photo_name;
        $(el).summernote('editor.insertImage', url);
        $('#imageBoard > ul').append('<li><img src="'+url+'" width="480" height="auto"/></li>');*/
      },
      error: function(data){
    	  alert('오류'+data);
      }
    });
  }