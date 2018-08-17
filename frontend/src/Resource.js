import React, { Component } from 'react';
import './Resource.css';

class Resource extends Component {

  constructor(props) {
      super(props);
      this.state = {occupyHours: 2};
  }

  render() {
    var isLocked = this.props.lock;
    var blockClassName = "resource-block" + (isLocked ? " busy" : "");
    return (
       <div className={blockClassName} >
          <h3>{this.props.resource.id}</h3>
          <p className="descr" >{this.props.resource.descr}</p>
           {isLocked ?
               <div>
                   <h3> OCCUPIED </h3>
                   <p> by <span className="locking-user">{this.props.lock.singles.lock_user.node.displayName}</span></p>
                    <p>until <span className="locking-user">{this.props.lock.node.release.time.hour}:{this.props.lock.node.release.time.minute}</span> </p>
		          
		           {(this.props.username === this.props.lock.singles.lock_user.node.displayName) ? (<button type="button" onClick={(e) => this.props.onRelease()}>Release</button>) : ""}        
               </div>
             :
        	   <div>
                    <h5> FREE </h5>        	  
	        	    {this.props.username &&   	  
	                    <p className="occupy"> <button type="button" onClick={(e) => this.props.onOccupy(this.state.occupyHours)}>Occupy</button> 
	                        for <input type="text" value={this.state.occupyHours} onChange={e => this.setState({occupyHours: e.target.value})}/> hrs
	                    </p>
	        	     }
	           </div>          
           }
       </div>
    )
  }

}

export default Resource;
