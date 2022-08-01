function send(){
        let text = document.querySelector('#textarea').value;
        if(!text){
            alert('请输入内容');
            return ;
        }
        let item = document.createElement('div');
        item.className = 'item item-right';
        item.innerHTML = `<div class="bubble bubble-left">${text}</div><div class="avatar"><img src="https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=3313909130,2406410525&fm=15&gp=0.jpg" /></div>`;
        document.querySelector('.content').appendChild(item);
        document.querySelector('#textarea').value = '';
        document.querySelector('#textarea').focus();
        //滚动条置底
        let height = document.querySelector('.content').scrollHeight;
        document.querySelector(".content").scrollTop = height;
    }
