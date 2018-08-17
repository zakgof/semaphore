import React, { Component } from 'react';
import Resource from './Resource.js';
import Login from './Login.js';
import axios from 'axios';
import jwtDecode from 'jwt-decode';
import './App.css';

class AjaxService {

   static endpoint = "https://q9he9w9ps6.execute-api.us-east-1.amazonaws.com/Prod";
   request(command, data, onSuccess, onError) {
      axios.post(AjaxService.endpoint, {command: command, data: data})
      .then(res => {
          console.log("ajax " + command + " returned " + JSON.stringify(res));
          onSuccess(res.data);                        
      })
      .catch(err => {
          console.log("ajax error " + JSON.stringify(err));
          onError(err.message);
      });
   }

}

class App extends Component {

  constructor(props) {
      super(props);
      this.state = {resources: []};
      this.ajax = new AjaxService();
  }

  request(command, data, onSuccess) {
      var that = this;
      this.setState({"error" : null, "status": "Requesting data..."});
      this.ajax.request(command, data, data => {
        onSuccess(data);
        that.setState({"error" : data.error, "status": null});
        var jwt = localStorage.getItem('semaphore.jwt');
        if (jwt) {
      	    this.setState({jwt: jwt, username: jwtDecode(jwt).username});  
        }  
      }, err => {
        that.setState({"error" : err, "status": null});
      });
  }

  componentDidMount() {
      this.request("get", null, data => this.setState({resources: data.resources }));
  }

  render() {
	 var that = this;
     console.log("Render, state = " + JSON.stringify(this.state));
     return (  
         <div>
		      <div className="login">    	 
			     <Login username={this.state.username} onLogin={this.telegramLogin.bind(this)} onLogoff={this.logoff.bind(this)} />
		     </div>
	         <div className="status">
			     {this.state.status && <span>{this.state.status}</span>}
			     {this.state.error  && <span>{this.state.error}</span>}
		     </div>
		     
	    	 {this.state.resources.map((k,v) => {
    		      return <Resource key={k.node.id} resource={k.node} lock={k.singles.resource_lock} username={this.state.username} onOccupy={d =>that.occupy(k.node.id, d)} onRelease={() =>that.release(k.node.id)} />
    		 })}
	    	 {this.state.username === "Oleksandr Zakusylo" && this.renderAdmin()}
	    	 
	    	 <p><a href="https://web.telegram.org/#/im?p=@lgc_semaphore_bot">Add this bot to get notifications</a></p>
     )
  }
  
  renderAdmin() {
	  return (		
	      <div className="admin">
		       Command <input type="text" value={this.state.adminCommand} onChange={e=>this.setState({adminCommand: e.target.value})} /> 
		       Params  <textarea height="8" onChange={e=>this.setState({adminData: e.target.value})} >{this.state.adminData}</textarea>
			   <button onClick={e =>this.adminSend()}>Send</button>
		  </div>			  
	  )
  }
  
  logoff() {
	  localStorage.removeItem("semaphore.jwt");
	  this.setState({"username" : null, "jwt" : null});
  }
  
  telegramLogin(tele) {
	  var that = this;
	  console.log("this " + this);
	  console.log("telegram " + JSON.stringify(tele));
	  this.request("login", tele, data => {
		  console.log("RECEIVED JWT "  + JSON.stringify(jwtDecode(data.jwt)));
		  localStorage.setItem('semaphore.jwt', data.jwt);
		  that.forceUpdate();
	  });
  }

  occupy(resourceId, hours) {
     var that = this;
     this.request("resource.lock", {jwt: this.state.jwt, resourceId: resourceId, minutes: hours * 60}, e => that.request("get", null, data => that.setState({resources: data.resources })));
  }
  
  release(resourceId, hours) {
     var that = this;
     this.request("resource.release", {jwt: this.state.jwt, resourceId: resourceId}, e => that.request("get", null, data => that.setState({resources: data.resources })));
  }
  
  adminSend() {
     this.request(this.state.adminCommand, JSON.parse(this.state.adminData), e => {});
  }

}

export default App;
