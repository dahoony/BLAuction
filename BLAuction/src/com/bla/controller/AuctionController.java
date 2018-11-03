
package com.bla.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.bla.biz.AuctionBiz;
import com.bla.biz.BiddingBiz;
import com.bla.biz.MemberBiz;
import com.bla.biz.PhotoBiz;
import com.bla.biz.SuccessfulBidBiz;
import com.bla.util.FileSave;
import com.bla.vo.AuctionVO;
import com.bla.vo.BiddingVO;
import com.bla.vo.ListVO;
import com.bla.vo.MemberVO;
import com.bla.vo.PhotoVO;
import com.bla.vo.SuccessfulBidVO;

@Controller
public class AuctionController {
	@Resource(name = "abiz")
	AuctionBiz abiz;

	@Resource(name = "pbiz")
	PhotoBiz pbiz;

	@Resource(name = "bbiz")
	BiddingBiz bbiz;

	@Resource(name = "sbiz")
	SuccessfulBidBiz sbiz;

	@Resource(name = "mbiz")
	MemberBiz mbiz;

	// 경매 등록 페이지 넘기기
	@RequestMapping("/createAuction.bla")
	public ModelAndView createAuction(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		try {			
			int member_id = (Integer)request.getSession().getAttribute("member_id");
			
			mv.setViewName("main");
			mv.addObject("centerpage", "auction/register");
		}catch(Exception e){
			mv.setViewName("redirect:/login.bla");
		}
		
		return mv;
	}

	// 경매 등록 성공 페이지 넘기기
	@RequestMapping("/createAuction_success.bla")
	public ModelAndView createAuction_success() {

		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		mv.addObject("centerpage", "auction/success");
		return mv;
	}

	// 경매 등록 실시
	@RequestMapping("/createAuctionimpl.bla")
	@ResponseBody
	public JSONObject createAuctionimpl(MultipartHttpServletRequest multi) {// 원래면 매개변수로 받음

		System.out.println("###################### CREATING AUCTION !!! ######################");
		String date = multi.getParameter("due_date");
		date = date.replaceAll("T", " ");
		// Get time
		SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date newdate = null;
		Long duedate = 0l;
		try {
			newdate = form.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		duedate = newdate.getTime();
		int member_id = 0;
		String seller_account = null;
		// Session에서 정보 추출
		HttpSession session = multi.getSession();
		if(session.getAttribute("member_id")!=null && session.getAttribute("member_account") != null) {
			member_id = (Integer) session.getAttribute("member_id");
			seller_account = (String) session.getAttribute("member_account");
		}
		

//			// Auction 객체 생성 [공통]
		AuctionVO auction = new AuctionVO();
		Long register_date = System.currentTimeMillis();
		auction.setDuedate(duedate);
		Long start_price = Long.parseLong(multi.getParameter("start_price"));
		int auction_type = Integer.parseInt(multi.getParameter("type"));
		auction.setMember_id(member_id);
		auction.setType(auction_type);
		auction.setAuct_title(multi.getParameter("auct_title"));
		auction.setStart_price(start_price);
		auction.setSeller_account(seller_account);
		auction.setCategory_id(Integer.parseInt(multi.getParameter("category_id")));
		auction.setDescription(multi.getParameter("description"));
		auction.setRegister_date(register_date);
		auction.setTag(multi.getParameter("registerTags"));

		// DB 저장 및 DB select로 auction_id 추출
		Long down_price = 0L;
		int down_term = 0;
		int auct_id = 0;

		try {
			// 내림경매
			if ((multi.getParameter("type")).equals("2")) {
				down_price = Long.parseLong(multi.getParameter("down_price"));
				down_term = Integer.parseInt(multi.getParameter("down_term"));
				auction.setDown_price(down_price);
				auction.setDown_term(down_term);
				auction.setAuction_status("proceeding");
				abiz.registerDown(auction);
				auct_id = abiz.get(register_date);
			} else {
				abiz.register(auction);
				auct_id = abiz.get(register_date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// 사진 저장 경로 설정
		String root = multi.getSession().getServletContext().getRealPath(File.separator);
		
		String path = root + "resources"+File.separator+"thumbnail";
		String newFileName = ""; // 업로드 되는 파일명

		// 경로 폴더 없을 시 폴더 생성
		File dir = new File(path);
		if (!dir.isDirectory()) {
			dir.mkdir();
		}

		// 넘어온 데이터에서 파일추출 후 저장
		Iterator<String> files = multi.getFileNames();
		int i = 0;
		while (files.hasNext()) {
			String uploadFile = files.next();
			if (uploadFile.equals("files"))
				break;
			MultipartFile mFile = multi.getFile(uploadFile);
			String fileName = mFile.getOriginalFilename();
			if (fileName.length() == 0) {
				break;
			}
			String typeName = fileName.substring(fileName.indexOf("."));
			newFileName = auct_id + "_" + i + typeName.toLowerCase();
			try {
				FileSave.save(path, mFile, newFileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			i++;

			// 사진 DB 저장
			PhotoVO photo = new PhotoVO();
			photo.setAuct_id(auct_id);
			photo.setPhoto_name(newFileName);
			photo.setPhoto_path("resources"+File.separator+"thumbnail"+File.separator);
			System.out.println("PHOTO : " + photo);

			try {
				pbiz.register(photo);
				System.out.println("PHOTO UPLOADED");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// JSON 값으로 Client에 return
		JSONObject json = new JSONObject();
		try {
			json.put("auction_id", auct_id);
			json.put("seller_id", member_id);
			json.put("due_date", duedate);
			json.put("start_price", start_price);
			json.put("auction_type", auction_type);
			json.put("down_price", down_price);
			json.put("down_term", down_term);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return json;
	}

	// 옥션 상세 페이지 넘기기
	@RequestMapping("/auctiondetail.bla")
	public ModelAndView auctiondetail(HttpServletRequest request) {
		//로그인된 멤버 아이디 가져오기
		ModelAndView mv = new ModelAndView();
		int member_id = 0;
		try {			
			member_id = (Integer)request.getSession().getAttribute("member_id");
			
			mv.setViewName("main");
		}catch(Exception e){
			mv.setViewName("redirect:/login.bla");
			return mv;
		}
		Map<String, Integer> map = new HashMap<>();
		map.put("member_id", member_id);
		Integer auct_id = Integer.parseInt(request.getParameter("auctionid"));
		map.put("auct_id",auct_id);
		AuctionVO auction = null;
		ArrayList<PhotoVO> photos = null;
		String name = (String)request.getSession().getAttribute("name");

		
		try {
			auction = abiz.get(auct_id);
			photos = pbiz.getAll(auct_id);
			System.out.println(photos);
			Long cur_priceL = bbiz.selectBidMaxPrice(auction);
			if (cur_priceL == null) {
				cur_priceL = auction.getStart_price();
			}
			System.out.println(cur_priceL);

			Double cur_price = cur_priceL.doubleValue();
			cur_price = Math.round(cur_price * 1000) / 1000.0;

			// 카테고리 구하기
			String category = "";
			switch (auction.getCategory_id()) {
			case 1:
				category = "의류 / 잡화";
				break;
			case 2:
				category = "뷰티 / 미용";
				break;
			case 3:
				category = "스포츠 / 레저";
				break;
			case 4:
				category = "디지털 / 가전";
				break;
			case 5:
				category = "생활 / 가구";
				break;
			case 6:
				category = "기타";
				break;
			}

			// 옥션타입 구하기
			String auction_type = "";
			switch (auction.getType()) {
			case 1:
				auction_type = "올림경매";
				break;
			case 2:
				auction_type = "내림경매";
				break;
			case 3:
				auction_type = "비밀경매";
				break;
			}

			// 마감일자 구하기
			String due_date = new SimpleDateFormat("yyyy년 MM월 dd일 hh시 mm분")
					.format(new Date((Long) auction.getDuedate()));
			//현재 내가 입찰한 최고가 
			long memberMaxPrice = 0;
			try {
				memberMaxPrice = bbiz.selectMemberMaxPrice(map);
			}catch(Exception e) {
				
			}
			mv.addObject("memberMaxPrice",memberMaxPrice);
			mv.addObject("name",name);
			mv.addObject("auction", auction);
			mv.addObject("cur_price", cur_price);
			mv.addObject("category", category);
			mv.addObject("auction_type", auction_type);
			mv.addObject("due_date", due_date);
			mv.addObject("timestamp", (Long) auction.getDuedate());
			try {
				mv.addObject("photo1", photos.get(0).getPhoto_path() + photos.get(0).getPhoto_name());
				mv.addObject("photo2", photos.get(1).getPhoto_path() + photos.get(1).getPhoto_name());
			} catch (Exception e) {

			}
			mv.addObject("centerpage", "auction/detail");
		} catch (Exception e) {
			e.printStackTrace();
			mv.addObject("centerpage", "auction/detail");
		}
		return mv;
	}

	// 옥션 비딩 리스트 SELECT AJAX로 보냄
	@RequestMapping("/auctionbidlist.bla")
	public void auctionbidlist(HttpServletRequest request, HttpServletResponse response) {
		// view에서 auct_id를 받아온다.
		int auct_id;
		try {
			auct_id = Integer.parseInt(request.getParameter("auction_id"));
		}catch(Exception e) {
			return;
		}


		// 입찰 해당 auct_id를 받아서 Bidding 객체를 가져온다.
		ArrayList<BiddingVO> biddings = null;
		// 해당하는 입찰자의 정보를 가져온다.
		MemberVO bid_member = null;

		// json 배열과 객체 선언
		JSONObject jo = null;
		JSONArray ja = null;

		// json 넘겨주기위함
		PrintWriter out = null;
		response.setContentType("text/json;charset=utf-8");

		try {
			biddings = bbiz.selectAuctionBiddingList(auct_id);
			ja = new JSONArray();
			for (BiddingVO bid : biddings) {
				jo = new JSONObject();
				// 입찰가, 입찰시간, 컨펌 상태를 get으로 꺼내와서 json 저장
				jo.put("bid_price", bid.getPrice());
				jo.put("bid_time", bid.getTime());
				jo.put("bid_conf_status", bid.getBid_conf_status());
				// 입찰자 정보 가져오기, 입찰자 이름만 가져와서 json 객체화
				bid_member = mbiz.get(bid.getMember_id());
				jo.put("bid_member_name", bid_member.getName());

				ja.add(jo);
			}
			out = response.getWriter();
			out.print(ja.toJSONString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 각종 카테고리 리스트 뿌려주기/////////////////////////////////
	@RequestMapping("/main.bla")
	public ModelAndView allCategory(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		HttpSession session = request.getSession();
		mv.setViewName("main");
		ArrayList<ListVO> down_list = new ArrayList<ListVO>();
		ArrayList<ListVO> time_list = new ArrayList<ListVO>();
		ArrayList<AuctionVO> auction_list = null;

		
		
		try {
			// TIME_LIST 작업
			auction_list = abiz.getByDuedate();
			
			Iterator<AuctionVO> tmp = auction_list.iterator();
			int i = 0;
			while (tmp.hasNext()) {
				AuctionVO auctionVO = (AuctionVO) tmp.next();
				
				if(auctionVO.getAuction_address() == null) 
					continue;
				
				
				if(i == 4) break;
				if(auctionVO.getType() == 2) {
					continue;
				}
				
				ListVO newlist = new ListVO();
				String due_date = new SimpleDateFormat("MM월 dd일 HH:mm")
						.format(new Date((Long) auctionVO.getDuedate()));
				PhotoVO photo1 = null;
				PhotoVO photo2 = null;
				try {
					photo1 = pbiz.getAll(auctionVO.getAuct_id()).get(0);
					newlist.setPhoto_path_1(photo1.getPhoto_path() + photo1.getPhoto_name());
					photo2 = pbiz.getAll(auctionVO.getAuct_id()).get(1);
					newlist.setPhoto_path_2(photo2.getPhoto_path() + photo2.getPhoto_name());
				} catch (Exception e) {

				}
				newlist.setAuction(auctionVO);
				newlist.setDuedate(due_date);
				newlist.setMax_price(bbiz.selectBidMaxPrice(auctionVO));
				if (newlist.getMax_price() == null) {
					newlist.setMax_price(auctionVO.getStart_price());
				}
				time_list.add(newlist);
				i++;
			}
			
			// down_auction list 작업
			auction_list.clear();
			auction_list = abiz.getByType(2);
			i = 0;
			Iterator<AuctionVO> itr = auction_list.iterator();
			while (itr.hasNext()) {
				AuctionVO auctionVO = (AuctionVO) itr.next();
				if(auctionVO.getAuction_address() == null) 
					continue;
				
				
				if(i == 4) break;
				
				ListVO newlist = new ListVO();
				String due_date = new SimpleDateFormat("MM월 dd일 HH:mm")
						.format(new Date((Long) auctionVO.getDuedate()));
				PhotoVO photo1 = null;
				PhotoVO photo2 = null;
				try {
					photo1 = pbiz.getAll(auctionVO.getAuct_id()).get(0);
					newlist.setPhoto_path_1(photo1.getPhoto_path() + photo1.getPhoto_name());
					photo2 = pbiz.getAll(auctionVO.getAuct_id()).get(1);
					newlist.setPhoto_path_2(photo2.getPhoto_path() + photo2.getPhoto_name());
				}catch(Exception e) {
					
				}
				newlist.setAuction(auctionVO);
				newlist.setDuedate(due_date);
				newlist.setMax_price(bbiz.selectBidMaxPrice(auctionVO));
				if (newlist.getMax_price() == null) {
					newlist.setMax_price(auctionVO.getStart_price());
				}
				down_list.add(newlist);
				i++;
			}

			mv.addObject("down_list", down_list);
			mv.addObject("time_list", time_list);
			mv.addObject("centerpage", "center");
		} catch (Exception e) {
			e.printStackTrace();
			mv.addObject("centerpage", "center");
		}

		System.out.println("###################### GET DOWN AUCTION ######################");
		Iterator<ListVO> itr = down_list.iterator();
		while (itr.hasNext()) {
			ListVO listVO = (ListVO) itr.next();
			System.out.println(listVO);
		}
		
		System.out.println("###################### GET 마감임박 ######################");
		Iterator<ListVO> itr_ = time_list.iterator();
		while (itr_.hasNext()) {
			ListVO listVO = (ListVO) itr_.next();
			System.out.println(listVO);
		}

		return mv;
	}

	@RequestMapping("/category.bla")
	public ModelAndView category(HttpServletRequest request) {
		//마감임박, 등록 순서 데로 정렬
		String listKind = request.getParameter("listKind");
		if(listKind == null) {
			
		}
		
		
		int category_id = Integer.parseInt(request.getParameter("category"));
		ArrayList<ListVO> list = new ArrayList<ListVO>();
		ArrayList<AuctionVO> auction_list = null;

		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		try {
			auction_list = abiz.getByCategory(category_id);

			int listSize = auction_list.size();
			/*//listSize가 4이상일 때 
			if(listSize > 4)
				listSize = 4;*/
			
			for (int i = 0; i < listSize; i++) {
				if(auction_list.get(i).getAuction_address() == null) {
					continue;
				}
				ListVO newlist = new ListVO();
				String due_date = new SimpleDateFormat("MM월 dd일 HH:mm")
						.format(new Date((Long) auction_list.get(i).getDuedate()));
				PhotoVO photo1 = null;
				PhotoVO photo2 = null;
				try {
					photo1 = pbiz.getAll(auction_list.get(i).getAuct_id()).get(0);
					newlist.setPhoto_path_1(photo1.getPhoto_path() + photo1.getPhoto_name());
					photo2 = pbiz.getAll(auction_list.get(i).getAuct_id()).get(1);
					newlist.setPhoto_path_2(photo2.getPhoto_path() + photo2.getPhoto_name());
				} catch (Exception e) {

				}
				newlist.setAuction(auction_list.get(i));
				newlist.setDuedate(due_date);
				newlist.setMax_price(bbiz.selectBidMaxPrice(auction_list.get(i)));
				if (newlist.getMax_price() == null) {
					newlist.setMax_price(auction_list.get(i).getStart_price());
				}
				list.add(newlist);
			}

			mv.addObject("list", list);
			mv.addObject("category_id", category_id);
			mv.addObject("centerpage", "auction/category");
		} catch (Exception e) {
			e.printStackTrace();
			mv.addObject("centerpage", "auction/category");
		}

		Iterator<ListVO> itr = list.iterator();
		while (itr.hasNext()) {
			ListVO listVO = (ListVO) itr.next();
			System.out.println(listVO);
		}

		return mv;
	}
	
	
	@RequestMapping("/allCategory.bla")
	public ModelAndView allcategory(HttpServletRequest request) {
		ArrayList<ListVO> list = new ArrayList<ListVO>();
		ArrayList<AuctionVO> auction_list = null;

		System.out.println("###################### GET ALL ######################");

		ModelAndView mv = new ModelAndView();
		mv.setViewName("main");
		try {
			auction_list = abiz.get();
			
			int listSize = auction_list.size();
			//listSize가 4이상일 때 
//			if(listSize > 4)
//				listSize = 4;
			for (int i = 0; i < listSize; i++) {
				if(auction_list.get(i).getAuction_address() == null) {
					continue;
				}
				
				ListVO newlist = new ListVO();
				String due_date = new SimpleDateFormat("MM월 dd일 HH:mm")
						.format(new Date((Long) auction_list.get(i).getDuedate()));
				PhotoVO photo1 = null;
				PhotoVO photo2 = null;
				try {
					photo1 = pbiz.getAll(auction_list.get(i).getAuct_id()).get(0);
					newlist.setPhoto_path_1(photo1.getPhoto_path() + photo1.getPhoto_name());
					photo2 = pbiz.getAll(auction_list.get(i).getAuct_id()).get(1);
					newlist.setPhoto_path_2(photo2.getPhoto_path() + photo2.getPhoto_name());
				}catch(Exception e) {
					
				}
				newlist.setAuction(auction_list.get(i));
				newlist.setDuedate(due_date);
				newlist.setMax_price(bbiz.selectBidMaxPrice(auction_list.get(i)));
				if (newlist.getMax_price() == null) {
					newlist.setMax_price(auction_list.get(i).getStart_price());
				}
				list.add(newlist);
			}

			mv.addObject("list", list);
			mv.addObject("centerpage", "auction/allCategory");
		} catch (Exception e) {
			e.printStackTrace();
			mv.addObject("centerpage", "auction/allCategory");
		}

		Iterator<ListVO> itr = list.iterator();
		while (itr.hasNext()) {
			ListVO listVO = (ListVO) itr.next();
			System.out.println(listVO);
		}

		return mv;
	}

	// 입찰할 때 Bidding 정보 INSERT
	@RequestMapping("/biddingimpl.bla")
	public void biddingimpl(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		// session에 저장시킨 member_id와 bidder_account를 받는다.

		int member_id = (int) session.getAttribute("member_id");
		String bidder_account = (String) session.getAttribute("member_account");

		// request로 price, time, auct_id를 넘겨받는다.
		long price = Long.parseLong(request.getParameter("price"));
		long time = Long.parseLong(request.getParameter("time"));
		int auct_id = Integer.parseInt(request.getParameter("auction_id"));

		// BiddingVO bid = new BiddingVO(member_id,auct_id,price,time,bidder_account);
		// BiddingVO bid = new BiddingVO(3, 1, 2l, time,
		// "0x9671652cf6fba11f7576b341b95bff03ad27d581");
		BiddingVO bid = new BiddingVO(member_id, auct_id, price, time, bidder_account);

		// json 배열과 객체 선언
		JSONObject jo = new JSONObject();

		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;
		// DB insert

		// 옥션 상태도 바꿔줘야함,
		AuctionVO auct_update = new AuctionVO();
		auct_update.setAuct_id(auct_id);
		auct_update.setAuction_status("proceeding");
		try {
			bbiz.register(bid);

			abiz.updateStatus(auct_update);
			out = response.getWriter();
			jo.put("isBid", true);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jo.put("isBid", false);
		} finally {
			out.print(jo.toJSONString());
			out.close();
		}

	}

	// 내가 입찰한 list SELECT
	@RequestMapping("/mybiddinglist.bla")
	public void mybiddinglist(HttpServletRequest request, HttpServletResponse response) {
		// 먼저 bidding 테이블에서 member_id로 auct_id를 찾고,
		// auct_id로 경매가 무엇이 있는 지 select 후 title과 사진 가져오기,
		// 가져온 auct_id로 bidding의 최고가, member_id로의 최고가를 구하시오..
		HttpSession session = request.getSession();
		int member_id = (Integer) session.getAttribute("member_id");
		
		ArrayList<Integer> auct_ids = null;
		AuctionVO auct = null;
		ArrayList<PhotoVO> photos = null;

		Map<String, Integer> map = new HashMap<>();
		map.put("member_id", member_id);

		// json 배열과 객체 선언
		JSONObject jo = null;
		JSONArray ja = null;

		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;

		try {
			// 회원이 입찰한 경매 id들(중복제거)
			auct_ids = bbiz.selectAuctIdByMemberId(member_id);
			ja = new JSONArray();
			// 회원이 입찰한 경매의 최고가와 그 경매에서 내가 입찰한 최고가 가져오기.
			for (Integer auct_id : auct_ids) {
				// json 객체 및 배열화 해서 AJAX로 내보내야할듯!
				auct = abiz.get(auct_id);
				jo = new JSONObject();
				jo.put("auct_id", auct_id);
				jo.put("title", auct.getAuct_title());
				jo.put("auction_type", auct.getType());
				jo.put("seller_id", auct.getMember_id());
				jo.put("auction_status", auct.getAuction_status());
				jo.put("auction_address", (String)auct.getAuction_address());
				// 사진도 Photo테이블에서 경로랑 이름 들고와야함..
				photos = pbiz.getAll(auct_id);

				int i = 0;
				for (PhotoVO photoVO : photos) {
					String pathKey = "photoPath" + i;
					String nameKey = "photoName" + i;
					jo.put(pathKey, photoVO.getPhoto_path());
					jo.put(nameKey, photoVO.getPhoto_name());
					i++;
				}
				map.put("auct_id", auct_id);
				jo.put("bidMaxPrice", bbiz.selectBidMaxPrice(auct));
				jo.put("memberMaxPrice", bbiz.selectMemberMaxPrice(map));

				// json 객체 및 배열화 해서 AJAX로 내보내야할듯!
				ja.add(jo);
			}

			out = response.getWriter();
			out.print(ja.toJSONString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 내가 낙찰한 물품 리스트 SELECT
	@RequestMapping("/mysuccessbidlist.bla")
	public void mysuccessbidlist(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();

		int member_id = (Integer) session.getAttribute("member_id");

		Map<String, Integer> map = new HashMap<>();
		map.put("member_id", member_id);

		ArrayList<SuccessfulBidVO> successfulBids = null;

		// json 배열과 객체 선언
		JSONObject jo = new JSONObject();

		JSONArray failJa = new JSONArray();
		JSONArray successfulJa = new JSONArray();

		JSONObject failJo = null;
		JSONObject successfulJo = null;

		// json 넘겨주기위함
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;

		// 내가 입찰하고, auction_status가 end인 auction
		ArrayList<Integer> auct_ids = null;
		ArrayList<AuctionVO> aucts = new ArrayList<>();

		ArrayList<PhotoVO> photos = null;

		try {
			// 내가 입찰한 auct_id를 Bidding 테이블에서 가져온다.
			// 내가 입찰하고, auction_status가 end인 auction들을 가져온다.
			auct_ids = bbiz.selectAuctIdByMemberId(member_id);
			for (Integer auct_id : auct_ids) {
				AuctionVO auction = abiz.get(auct_id);
				if (auction.getAuction_status().equals("end")) {
					aucts.add(auction);
				}
			}

			// 가져와서 내가 입차한 최고가격과 auction의 최고가를 비교하여서
			// 같지 않는 auction들만 담아서 보낸다.
			for(AuctionVO auction : aucts) {
				map.put("auct_id", auction.getAuct_id());
				Long bidMaxPrice = bbiz.selectBidMaxPrice(auction);
				Long memberBidMaxPrice = bbiz.selectMemberMaxPrice(map);
				if (bidMaxPrice != memberBidMaxPrice) {
					failJo = new JSONObject();
					failJo.put("auct_id", auction.getAuct_id());
					photos = pbiz.getAll(auction.getAuct_id());
					int i = 0;
					for (PhotoVO photoVO : photos) {
						System.out.println(photoVO);
						String pathKey = "photoPath" + i;
						String nameKey = "photoName" + i;
						failJo.put(pathKey, photoVO.getPhoto_path());
						failJo.put(nameKey, photoVO.getPhoto_name());
						i++;
					}
					failJo.put("auct_title", auction.getAuct_title());
					failJo.put("auct_address", auction.getAuction_address());
					failJo.put("my_bid_price", memberBidMaxPrice);
					failJa.add(failJo);
				}
			}

			// 처음 한 쿼리로
			// Bidding 테이블에서 member_id가 가지고 있는 bid_id를 가져와서
			// SUCCESSFUL_BID 테이블에서 bid_id가 포함되어있는 것에서 successfulbid 가져오기

			successfulBids = sbiz.selectMySuccessfulBid(member_id);

			// successfulbidvo에서 bid_id로 Bidding table에서 price 가져오기
			Long price = 0l;
			// 다른 쿼리로 auct_id 로 Photo 테이블에서 사진 가져오고

			// 다른 쿼리로 auct_id 로 member_id를 가져오고
			int auct_member_id = 0;
			// 다른 쿼리로 member_id로 판매자 이름, 판매자 전화번호
			MemberVO sellerInfo = null;

			for (SuccessfulBidVO successfulBid : successfulBids) {

				successfulJo = new JSONObject();
				price = bbiz.get(successfulBid.getBid_id()).getPrice();
				photos = pbiz.getAll(successfulBid.getAuct_id());
				AuctionVO auction = abiz.get(successfulBid.getAuct_id());
				int successfulAuct_id = auction.getAuct_id();
				auct_member_id = abiz.selectMemberIdByAuct(successfulBid.getAuct_id());
				sellerInfo = mbiz.get(auct_member_id);
				successfulJo.put("auct_id", successfulAuct_id);
				successfulJo.put("title", auction.getAuct_title());
				successfulJo.put("auct_address", auction.getAuction_address());
				successfulJo.put("price", price);

				int i = 0;
				for (PhotoVO photoVO : photos) {
					String pathKey = "photoPath" + i;
					String nameKey = "photoName" + i;
					successfulJo.put(pathKey, photoVO.getPhoto_path());
					successfulJo.put(nameKey, photoVO.getPhoto_name());
					i++;
				}
				
				successfulJo.put("seller_id", auct_member_id);
				successfulJo.put("seller_name", sellerInfo.getName());
				successfulJo.put("seller_phone", sellerInfo.getPhone());
				successfulJo.put("delivery_code", successfulBid.getDelivery_code());
				successfulJo.put("delivery_status", successfulBid.getDelivery_status());
				successfulJo.put("company_code", successfulBid.getCompany_code());

				successfulJa.add(successfulJo);
			}

			out = response.getWriter();
			jo.put("failBid", failJa);
			jo.put("successfulBid", successfulJa);

			out.print(jo);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 내가 올린 경매 리스트 SELECT, AJAX 써야함
	@RequestMapping("/myauctionlist.bla")
	public void myauctionlist(HttpServletRequest request, HttpServletResponse response) {
		// Auction테이블에서 member_id로 select (ArrayList<AuctionVO>)
		HttpSession session = request.getSession();

		int member_id = (Integer) session.getAttribute("member_id");

		// seller_id를 넘겨 받아서 null이 아니면 member_id에 seller_id를 넣고 null일 경우 안넣는다.
		String seller_id = request.getParameter("seller_id");
		if (seller_id != null) {
			member_id = Integer.parseInt(seller_id);
		}

		Map<String, Integer> map = new HashMap<>();
		map.put("member_id", member_id);

		ArrayList<AuctionVO> auctions = null;

		// json 객체 배열 선언
		JSONObject jo = new JSONObject();

		JSONArray beforeJa = new JSONArray();
		JSONArray proceedingJa = new JSONArray();
		JSONArray endJa = new JSONArray();
		JSONArray cancelJa = new JSONArray();
		JSONArray failbidJa = new JSONArray();

		JSONObject beforeJo = null;
		JSONObject proceedingJo = null;
		JSONObject endJo = null;
		JSONObject cancelJo = null;
		JSONObject failbidJo = null;

		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;

		try {
			// Auction 테이블에서 member_id로 내가 올린경매를 가져온다.
			// auct_id로 사진을 가져온다.
			// 경매 중-입찰전, 입찰 중, 낙찰, 취소 => 쿼리문 조건으로 날려서 따로 가지고와서 json화 한다.
			AuctionVO auction_member = new AuctionVO();
			auction_member.setMember_id(member_id);
			auctions = abiz.selectAuctionByMember(auction_member);

			for (AuctionVO auction : auctions) {
				int auct_id = auction.getAuct_id();
				if (auction.getAuction_status().equals("before")) {
					beforeJo = new JSONObject();
					// 입찰전 => 취소하기 버튼
					// 경매 사진과 타이틀도 가져와야함
					// before이라는 key로 JSON 배열에 JSON 객체를 넣는다.(status도)
					beforeJo.put("auct_id", auction.getAuct_id());
					beforeJo.put("auction_status", auction.getAuction_status());
					
					if(auction.getAuction_address() == null) {
						auction.setAuction_address("null");
					}
					beforeJo.put("auction_address", auction.getAuction_address());
					ArrayList<PhotoVO> photos = pbiz.getAll(auct_id);
					
					int i = 0;
					for (PhotoVO photoVO : photos) {
						String pathKey = "photoPath" + i;
						String nameKey = "photoName" + i;
						beforeJo.put(pathKey, photoVO.getPhoto_path());
						beforeJo.put(nameKey, photoVO.getPhoto_name());
						i++;
					}
					beforeJo.put("auct_title", auction.getAuct_title());
					beforeJo.put("start_price", auction.getStart_price());
					beforeJa.add(beforeJo);

				} else if (auction.getAuction_status().equals("proceeding")) {
					proceedingJo = new JSONObject();
					// 입찰중 => 재입찰하기 또는 내가 최고가일때 입찰 ㄴㄴ

					// 내림경매 일때는 bidding 테이블을 거치면 안됨(조건 걸어줘야한다)
					if (auction.getType() != 2) {
						// 비딩의 최고 가격을 가져온다.
						long bidMaxPrice = bbiz.selectBidMaxPrice(auction);
						proceedingJo.put("bidMaxPrice", bidMaxPrice);
					}
					if(auction.getAuction_address() == null) {
						auction.setAuction_address("null");
					}
					proceedingJo.put("auction_address", auction.getAuction_address());
					
					proceedingJo.put("auct_id", auction.getAuct_id());

					// 경매 사진과 타이틀도 가져와야함
					ArrayList<PhotoVO> photos = pbiz.getAll(auct_id);

					int i = 0;
					for (PhotoVO photoVO : photos) {
						String pathKey = "photoPath" + i;
						String nameKey = "photoName" + i;
						proceedingJo.put(pathKey, photoVO.getPhoto_path());
						proceedingJo.put(nameKey, photoVO.getPhoto_name());
						i++;
					}
					proceedingJo.put("auct_type", auction.getType());
					proceedingJo.put("auct_title", auction.getAuct_title());
					// 경매 마감 시간 보내기
					String due_date = new SimpleDateFormat("yyyy년 MM월 dd일 hh시 mm분")
							.format(new Date((Long) auction.getDuedate()));
					proceedingJo.put("dueDate", due_date);
					// proceeding이라는 key로 JSON 배열에 JSON 객체를 넣는다.(status도)
					proceedingJa.add(proceedingJo);
				} else if (auction.getAuction_status().equals("end")) {
					endJo = new JSONObject();
					// 낙찰 => 경매 완료

					// 경매 사진과 타이틀도 가져와야함
					ArrayList<PhotoVO> photos = pbiz.getAll(auct_id);

					endJo.put("auct_id", auction.getAuct_id());
					int i = 0;
					for (PhotoVO photoVO : photos) {
						String pathKey = "photoPath" + i;
						String nameKey = "photoName" + i;
						endJo.put(pathKey, photoVO.getPhoto_path());
						endJo.put(nameKey, photoVO.getPhoto_name());
						i++;
					}
					endJo.put("auct_title", auction.getAuct_title());
					endJo.put("auct_address", auction.getAuction_address());

					// 낙찰가, 낙찰된 회원의 이름, 전화번호, 주소 가져오기
					SuccessfulBidVO successfulBid = sbiz.oneSelectMySuccessfulBid(auct_id);
					BiddingVO bidding = bbiz.get(successfulBid.getBid_id());
					MemberVO successfulBidMember = mbiz.get(bidding.getMember_id());
					endJo.put("successfulBidPrice", bidding.getPrice());
					endJo.put("successfulBidMember_name", successfulBidMember.getName());
					endJo.put("successfulBidMemberPhone", successfulBidMember.getPhone());
					endJo.put("successfulBidAddress", successfulBidMember.getAddress());
					
					// 택배 운송장 번호를 SUCCESSFULBID에서 가져오기 auct_id로
					endJo.put("delivery_code", successfulBid.getDelivery_code());
					endJo.put("delivery_status", successfulBid.getDelivery_status());
					endJo.put("company_code", successfulBid.getCompany_code());

					// end이라는 key로 JSON 배열에 JSON 객체를 넣는다.(status도)

					endJa.add(endJo);
				} else if (auction.getAuction_status().equals("cancel")) {
					cancelJo = new JSONObject();
					// 취소 => 취소된 경매
					// 경매 사진과 타이틀도 가져와야함
					ArrayList<PhotoVO> photos = pbiz.getAll(auct_id);

					cancelJo.put("auct_id", auction.getAuct_id());

					int i = 0;
					for (PhotoVO photoVO : photos) {
						String pathKey = "photoPath" + i;
						String nameKey = "photoName" + i;
						cancelJo.put(pathKey, photoVO.getPhoto_path());
						cancelJo.put(nameKey, photoVO.getPhoto_name());
						i++;
					}
					cancelJo.put("auct_title", auction.getAuct_title());

					// cancel이라는 key로 JSON 배열에 JSON 객체를 넣는다.(status도)
					cancelJa.add(cancelJo);
				} else if (auction.getAuction_status().equals("failbid")) {
					failbidJo = new JSONObject();
					// 유찰 => 유찰된 경매
					// 경매 사진과 타이틀도 가져와야함
					ArrayList<PhotoVO> photos = pbiz.getAll(auct_id);

					failbidJo.put("auct_id", auction.getAuct_id());

					int i = 0;
					for (PhotoVO photoVO : photos) {
						String pathKey = "photoPath" + i;
						String nameKey = "photoName" + i;
						failbidJo.put(pathKey, photoVO.getPhoto_path());
						failbidJo.put(nameKey, photoVO.getPhoto_name());
						i++;
					}
					failbidJo.put("auct_title", auction.getAuct_title());

					// cancel이라는 key로 JSON 배열에 JSON 객체를 넣는다.(status도)
					failbidJa.add(failbidJo);
				}
			}

			jo.put("before", beforeJa);
			jo.put("proceeding", proceedingJa);
			jo.put("end", endJa);
			jo.put("cancel", cancelJa);
			jo.put("failbid", failbidJa);

			out = response.getWriter();
			out.print(jo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 판매자 페이지
	@RequestMapping("/sellerpage.bla")
	public ModelAndView sellerpage(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView();
		// seller_id를 불러와서 정보 넘겨주기.
		int member_id = Integer.parseInt(request.getParameter("seller_id"));

		MemberVO sellerInfo = null;
		try {
			sellerInfo = mbiz.get(member_id);
			mv.addObject("seller_info", sellerInfo);
			mv.addObject("centerpage", "user/sellerPage");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mv.setViewName("main");

		return mv;
	}

	// 판매자 페이지 후기 리스트
	@RequestMapping("/sellerReview.bla")
	public void sellerReview(HttpServletRequest request, HttpServletResponse response) {
		// 판매자 member_id를 들고온다.
		int seller_id = Integer.parseInt(request.getParameter("seller_id"));
		// member_id로 auct들을 가져오고,
		ArrayList<AuctionVO> aucts = null;
		// 해당 옥션의 auct_id, 사진, 타이틀 json화
		// auct_id로 successfulbid 에서 review를 들고온다.
		SuccessfulBidVO successfulBid = null;
		// reveiw json화.
		JSONArray ja = new JSONArray();
		JSONObject jo = null;
		
		//map 선언
		Map<String,Integer> map = new HashMap<>();
		map.put("member_id", seller_id);
		
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;
		
		try {
			aucts = abiz.selectEndAuctionByMemberId(map);
			
			for(AuctionVO auct : aucts) {
				successfulBid = sbiz.oneSelectMySuccessfulBid(auct.getAuct_id());
				//bidid를 가져와서 bidding 테이블에서 member_id와 낙찰가를 가져오고,
				//해당 member_id를 가져와서 낙찰자 이름 가져오기 
				if(successfulBid.getReview() != null) {
					jo = new JSONObject();
					BiddingVO bid = bbiz.get(successfulBid.getBid_id());
					
					MemberVO reviewer = mbiz.get(bid.getMember_id());
					
					jo.put("name",reviewer.getName());
					jo.put("price", bid.getPrice());
					jo.put("auct_title",auct.getAuct_title());
					jo.put("review", successfulBid.getReview());
					ArrayList<PhotoVO> photos = pbiz.getAll(auct.getAuct_id());
					
					jo.put("auct_id", auct.getAuct_id());

					int i = 0;
					for (PhotoVO photoVO : photos) {
						String pathKey = "photoPath" + i;
						String nameKey = "photoName" + i;
						jo.put(pathKey, photoVO.getPhoto_path());
						jo.put(nameKey, photoVO.getPhoto_name());
						i++;
					}
					ja.add(jo);
				}
				
			}
			out = response.getWriter();
			out.print(ja.toJSONString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 낙찰이 완료되고 물품을 보냈을 때 택배 운송장 번호를 입력.
	@RequestMapping("/deliveryimpl.bla")
	public void deliveryimpl(HttpServletRequest request, HttpServletResponse response) {
		int auct_id =Integer.parseInt(request.getParameter("auct_id"));
		String delivery_code = request.getParameter("delivery_code");
		String company_code = (String)request.getParameter("company_code");
		
		response.setContentType("text/json;charset=utf-8");
		JSONObject jo = new JSONObject();
		PrintWriter out = null;
		SuccessfulBidVO successfulbid = null;

		try {
			successfulbid = sbiz.oneSelectMySuccessfulBid(auct_id);
			successfulbid.setCompany_code(company_code);
			successfulbid.setDelivery_code(delivery_code);

			sbiz.modify(successfulbid);
			
			jo.put("company_code",company_code);
			jo.put("delivery_code", delivery_code);
			
			out = response.getWriter();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		out.println(jo);
	}
	
	@RequestMapping("/timestamp.bla")
	public void timeStamp(HttpServletRequest request, HttpServletResponse response) {
		JSONObject jo = new JSONObject();
		// json 넘겨주기위함
		PrintWriter out = null;

		jo.put("success", "success");

		try {
			out = response.getWriter();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.print(jo);
	}

	// 입찰 전 경매 취소하는 함수
	@RequestMapping("/auctioncancel.bla")
	public void auctioncancel(HttpServletRequest request, HttpServletResponse response) {
		
		int auct_id = Integer.parseInt(request.getParameter("auct_id"));
		String auct_status = request.getParameter("auct_status");
		
		response.setContentType("text/json;charset=utf-8");
		
		AuctionVO auction = new AuctionVO();
	
		try {
			auction.setAuct_id(auct_id);
			auction.setAuction_status("cancel");
			
			abiz.updateStatus(auction);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//후기 등록하는 함수
	@RequestMapping("/registerReview.bla")
	public void registerReview(HttpServletRequest request, HttpServletResponse response) {
		int auct_id = Integer.parseInt(request.getParameter("auct_id"));
		String review = request.getParameter("review");
		
		SuccessfulBidVO successfulBid = new SuccessfulBidVO();
		
		successfulBid.setAuct_id(auct_id);
		successfulBid.setReview(review);
		
		JSONObject jo = new JSONObject();
		
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;
		
		try {
			sbiz.updateReview(successfulBid);
			jo.put("result", "success");
			out = response.getWriter();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		out.print(jo);
		
	}
	
	// 검색창에서 검색한 결과를 내보내는 함수
	@RequestMapping("/searchimpl.bla")
	public ModelAndView searchimpl(HttpServletRequest request) {
		String searchText = request.getParameter("searchText");
		
		ArrayList<ListVO> searchList = new ArrayList<>();
		
		ArrayList<AuctionVO> aucts = null;
		
		ModelAndView mv = new ModelAndView("main");
		
		try {
			aucts = abiz.searchTitleOrTag(searchText);
			
			for(AuctionVO auct : aucts) {
				ListVO newList = new ListVO();
				if(auct.getTag() == null) {
					auct.setTag("");
				}
				newList.setAuction(auct);
				
				ArrayList<PhotoVO> photos = pbiz.getAll(auct.getAuct_id());
				String photo_path_1 = "";
				String photo_path_2 ="";
				for (int i = 0 ; i<photos.size();i++) {
					if(i==0)
						photo_path_1 = photos.get(i).getPhoto_path()+photos.get(i).getPhoto_name();
					else
						photo_path_2 = photos.get(i).getPhoto_path()+photos.get(i).getPhoto_name();

				}
				
				newList.setPhoto_path_1(photo_path_1);
				newList.setPhoto_path_2(photo_path_2);
				
				String due_date = new SimpleDateFormat("MM월 dd일 HH:mm")
						.format(new Date((Long) auct.getDuedate()));
				
				newList.setDuedate(due_date);
				long max_price = 0;
				try {
					max_price = bbiz.selectBidMaxPrice(auct);					
				}catch(Exception e) {
					max_price = auct.getStart_price();
				}
				newList.setMax_price(max_price);
				searchList.add(newList);
			}
			
			
			mv.addObject("result","success");
			mv.addObject("list",searchList);
			mv.addObject("centerpage","auction/search");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			mv.addObject("result","fail");
		}
		
		return mv;
	}

	// 사진 업로드 하는 함수(Photo)
	@RequestMapping("/photoupload.bla")
	public void photoUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request,
			HttpServletResponse response) {
		HttpSession session = request.getSession();

		// 회원id+timestamp => 이름 설정
		String imgName = file.getOriginalFilename();
		System.out.println(imgName);
		// String seller_id = (String) session.getAttribute("id");
		String seller_id = "1";
		String newImgName = seller_id + "_" + System.currentTimeMillis();

		newImgName += imgName.substring(imgName.indexOf("."));

		// 상대경로로 가져오기
		String path = session.getServletContext().getRealPath("/");
		path += "SummernoteImg";
		// 파일 저장
		FileSave.save(path, file, newImgName);
		// 디비 insert, select(photo_id, photo_name, photo_path)
		path = "SummernoteImg\\";
		PhotoVO photo = new PhotoVO(newImgName, path, 0);

		// photo_id, name, path JSON화 해서 AJAX로 보내기
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;
		try {
			out = response.getWriter();

			JSONObject jo = new JSONObject();

			jo.put("photo_name", photo.getPhoto_name());
			jo.put("photo_path", path);

			out.print(jo.toJSONString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			out.close();
		}

	}
	
	// 택배 상태 변경하는 함수
	@RequestMapping("/updateDeliveryStatus.bla")
	public void updateDeliveryStatus(HttpServletRequest request, HttpServletResponse response) {// db의 정보와 smartcontract log를 비교
		String status = request.getParameter("level");
		int auct_id = Integer.parseInt(request.getParameter("auct_id"));
		
		SuccessfulBidVO successfulBidVO = new SuccessfulBidVO();
		successfulBidVO.setAuct_id(auct_id);
		successfulBidVO.setDelivery_status(status);
		
		try {
			sbiz.updateDeliveryStatus(successfulBidVO);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}