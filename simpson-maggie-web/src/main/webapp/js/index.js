(function($) {
	var el, domain = '', path=[];
	
	function init(){
		mapElements();
		bindActions();
		refreshNav();
		refreshList();
	}

	//元素选择器
	function mapElements(){
		el = {
			$btnAdd:$('.js-add'),
			$table : $('.js-table'),
			$nav : $('.breadcrumb'),
			$dialogAdd:$('#AddModal'),
			$dialogDel:$('#DelModal'),
			$Title:$('#myModalLabel')
		}
	};

	//绑定事件
	function bindActions(){
		el.$btnAdd.on('click',openAdd);
		el.$table.on('click','.js-del',openDel);
		el.$table.on('click','.js-edit',openEdit);
		el.$table.on('click','.js-view',gotoFile);
		el.$dialogAdd.on('click','.js-submit',addFile);
		el.$dialogDel.on('click','.js-submit',deleteFile);
		el.$table.on('click','.gotoLink',visitSub);
		el.$nav.on('click','li',visitParent);
	};
	
	//拼url
	function SplitUrl(){
		var url = '';
		for(var i = 0; i < path.length; i++){
			url += path[i] + '/';
		}
		if('' == url || '/' != url.substring(0, 1)) {
			url = '/' + url;
		}
		return url;
	};

	// 刷新目录导航栏
	function refreshNav() {
		el.$nav.html('');
		for(var i = 0; i <= path.length; i++) {
			if(0 == i) {
				el.$nav.append('<li>Home</li>');
			}
			else {
				el.$nav.append('<li>' + path[i - 1] + '</li>');
			}
		}
	};
	// 刷新子文件列表
	function refreshList() {
		var url = SplitUrl();
		
		var data = {
			"path":url
		};
		$.ajax({
			"type":"POST",
			"url":domain + '/maggie/list',
			"data":data,
			"dataType":"json",
			"success":function(data) {
				if(data.code < 0) {
					if(null != data.msg && data.msg.length > 0) {
						alert(data.msg);
					}
					return;
				}
				data = data.data;
				var list = '';
				for(var i = 0; i < data.length; i++) {
					var item = data[i];
					list += '<tr><td class="sort">' + (i+1) + '</td>';
					if('file' == item.type) {
						list += '<td ><span class="js-view">' + item.name + '</span></td>';
						list += '<td class="operate"><a data-toggle="modal" data-type="view" data-target="#AddModal" class="js-edit">Update</a><a data-toggle="modal" data-target="#DelModal" class="js-del">Delete</a></td>';
					}
					else if('folder' == item.type) {
						list += '<td data-url="' + item.name + '"><b class="gotoLink">' + item.name + '</b></td><td></td>';
					}
					list += '</tr>';
				}
				el.$table.find('tbody').html(list);
				el.$dialogAdd.modal('hide');
				el.$dialogDel.modal('hide');
			}
		});
	};
	// 访问父目录
	function visitParent() {
		var i = $(this).index();
		console.log(i)
		while(i < path.length) {
			path.pop(i);
		}
		// 刷新目录导航栏和子文件列表
		refreshNav();
		refreshList();
	}
	// 访问子目录
	function visitSub() {
		path.push($(this).parent().data('url'));
		// 刷新目录导航栏和子文件列表
		refreshNav();
		refreshList();
	}
	function openAdd(){
		el.$dialogAdd.find('.js-url').val('');
		el.$dialogAdd.find('.js-modal').val('');
		el.$Title.html('新增');
	};
	function openEdit(){
		var $url = $(this).parent().siblings().find('.js-view').text();
		var url = SplitUrl();
		$('.js-url').val($url);
		el.$Title.html('编辑');
		viewFile(url+$url);
	};
	function openDel(){
		var $url = $(this).parent().siblings().find('.js-view').text();
		$('.js-url-del').html($url);
	};
	// 创建文件
	function addFile() {
		var url = SplitUrl();
		var $url = $('.js-url').val(), $modal = $('.js-modal').val();
		if('' != $url && '/' == $url.substring(0, 1)) {
			$url = $url.substring(1);
		}
		var data = {
			'path':$.trim(url+$url),
			'content':$.trim($modal)
		};
		$.ajax({
			"type":"POST",
            "url":domain + '/maggie/add',
            "data":data,
            "dataType":"json",
            "success":function(data) {
				if(data.code < 0) {
					if(null != data.msg && data.msg.length > 0) {
						alert(data.msg);
					}
					return;
				}
				refreshList();
			}
		});
	};
	// 查看文件文本
	function viewFile(url) {
		var data = {
			"path":url,
		};
		$('.js-modal').html('');
		$.ajax({
			type:"POST",
            url:domain + '/maggie/view',
            data:data,
            dataType:"text",
            success:function(d) {
				$('.js-modal').val(d);
			}
		});
	};
	// 删除文件
	function deleteFile() {
		var url = SplitUrl(),
		    $url = $('.js-url-del').text();
		    debugger;
		var data = {
			"path":url+$url
		};
		$.ajax({
			"type":"POST",
            "url":domain + '/maggie/delete',
            "data":data,
            "dataType":"json",
            "success": function(d) {
				if(d.code < 0) {
					if(null != d.msg && d.msg.length > 0) {
						alert(d.msg);
					}
					return;
				}
				refreshList();
            }
		});
	};

	//打开文件
	function gotoFile(){
		var url = domain+SplitUrl()+$(this).text();
		var i = url.lastIndexOf('.');
		var j = url.lastIndexOf('/');
		if(i > j && i != -1){
			url = url.substring(0,i);
		}
		window.open(url,'_blank');
	}
	//入口函数
	init();
})(jQuery);
