// 필요 작성 기능
// 1. 초기 데이터 뿌리기 ----- 서버사이드 에서 실행
// 2. 경매 입찰 ---- by web3 and ajax
// 3. 실시간으로 경매 시간 refresh. 서버시간 이용하기!
// 4. 내림 경매일 경우 시간이 지남에 따라 가격 변동 보여주기
// 5. 비밀 경매의 경우 한 사람이 여러번 입찰 하는 것이 필요 할 거 같음
//       --- 그러면 올림 경매일 경우에는??
// 6. 입찰 버튼 클릭시 입찰 모달 보여주기 ----- 경매 종류에 따라 다르게

var xmlHttp;
var date;
function srvTime() {
	  $.ajax({
	    type: 'GET',
	    cache: false,
	    url: '/timestamp.bla',
	    complete: function (req, textStatus) {
	      var dateString = req.getResponseHeader('Date');
	      if (dateString.indexOf('GMT') === -1) {
	        dateString += ' GMT';
	      }
	      date = new Date(dateString);
	      var timediff = dtA.getTime() - date.getTime();
	      
	      if(timediff <= 0){
	          $("#currentTimelimit").text('경매완료');
	          return;
	      }
	      
	      timediff /= 1000;
	      var s = '';
	      
	      if(parseInt(timediff/86400) >= 1){
	      	s += parseInt(timediff/86400) + '일 '
	          timediff %= 86400;
	      }
	      if(parseInt(timediff/3600) >= 1){
	      	s += parseInt(timediff/3600) + '시간 '
	          timediff %= 3600;
	      }
	      if(parseInt(timediff/60) >= 1){
	      	s += parseInt(timediff/60) + '분 '
	          timediff %= 60;
	      }
	      s += Math.floor(timediff) + '초';
	      $("#currentTimelimit").text(s);
	      $("#currentTimelimitModal").text(s);
	    }
	  });
}


function makeBid(){
	var price = 5//$('#??').text();
	var auction_id = 1//= "<%=(String)session.getAttribute('member_id')%>";
	alert("makeBid");
}

function getTimeStamp(d) {
	  var s =
	    leadingZeros(d.getMonth() + 1, 2) + '-' +
	    leadingZeros(d.getDate(), 2) + ' ' +

	    leadingZeros(d.getHours(), 2) + ':' +
	    leadingZeros(d.getMinutes(), 2) + ':' +
	    leadingZeros(d.getSeconds(), 2);

	  return s;
	}

function leadingZeros(n, digits) {
var zero = '';
n = n.toString();

if (n.length < digits) {
  for (i = 0; i < digits - n.length; i++)
    zero += '0';
}
return zero + n;
}


function makebidding(auction_id){
	var price = $("#suggestedPrice").val();
	var cur_price = Number($("#currentPrice").text());
	
	alert(price + "\n" + cur_price);
	
	price *= 1000;
	if(price ==0){
		alert("가격을 입력하세요");
		return;
	}else{
		
	}
	var params = {
			"price":price,
			"auction_id":auction_id,
			"time":date.getTime(),
		}
	
	$.ajax({
		type:'POST',
		url:'biddingimpl.bla', /* DB로 접근 */
		data:params,
		datatype:'json',
		success:function(data){
			alert(data)
		},
		error:function(data){
			alert(data)
		}
	})

	bidding(auction_id, price, date.getTime());
}