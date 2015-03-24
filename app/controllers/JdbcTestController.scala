package controllers

import java.util.Date

import play.api.db.DB
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import anorm.SqlParser._
import anorm._

/**
 * @author Hussachai Puripunpinyo
 */
object JdbcTestController extends Controller {

  case class Employee(empNo: Int, first: String, last: String, birthdate: Date, gender: String, hireDate: Date)

  val deptPopulationParser = (str("dept_no") ~ str("dept_name") ~ int("num_emps")).map{ case deptNo ~ deptName ~ numEmps =>
    (deptNo, deptName, numEmps)
  }
  val employeeParser = (int("emp_no") ~ str("first_name") ~ str("last_name") ~ date("birth_date")
    ~ str("gender") ~ date("hire_date")).map{ case no ~ first ~ last ~ dob ~ gender ~ hireDate =>
    Employee(no, first, last, dob, gender, hireDate)
  }
  val salaryParser = (double("salary")).map{case sal => sal}

  def sqlForDeptPopulation() = {
    SQL"""SELECT dept_no, dept_name, COUNT(*) AS num_emps FROM dept_emp de
        INNER JOIN departments d USING(dept_no) GROUP BY de.dept_no"""
  }

  def sqlForLastNEmployeeByDept(deptNo: String, n: Int) = {
    SQL"""SELECT emp_no, first_name, last_name, birth_date, gender, hire_date FROM employees em
         INNER JOIN dept_emp de USING (emp_no) WHERE de.dept_no = $deptNo
         ORDER BY em.hire_date DESC LIMIT $n"""
  }

  def sqlForAvgSalary(empNo: Int) = {
    SQL"""SELECT AVG(salary) AS salary FROM salaries WHERE emp_no = $empNo"""
  }

  case class DeptEmployeeStat(
    deptNo: String,
    deptName: String,
    totalEmployees: Int,
    employee: Employee,
    salary: Double
  )

  implicit val employeeFormat = Json.format[Employee]

  implicit val deptEmployeeStatFormat = Json.format[DeptEmployeeStat]

  def async1 = Action.async { request =>
    Future{
      DB.withConnection{ implicit c =>
        for{
          dept <- sqlForDeptPopulation().as(deptPopulationParser.*)
          emp <- sqlForLastNEmployeeByDept(dept._1, 10).as(employeeParser.*)
        } yield {
          DeptEmployeeStat(
            deptNo = dept._1,
            deptName = dept._2,
            totalEmployees = dept._3,
            employee = emp,
            salary = sqlForAvgSalary(emp.empNo).as(salaryParser.single)
          )
        }
      }
    }.map{ list =>
      Ok(Json.toJson(list))
    }
  }

  def async2 = Action.async { request =>
    def deptF() = Future(DB.withConnection{ implicit c =>
      sqlForDeptPopulation().as(deptPopulationParser.*)
    })
    def empF(empNo: String) = Future(DB.withConnection{ implicit c =>
      sqlForLastNEmployeeByDept(empNo, 10).as(employeeParser.*)
    })
    def salaryF(empNo: Int) = Future(DB.withConnection{ implicit c =>
      sqlForAvgSalary(empNo).as(salaryParser.single)
    })
    deptF().flatMap{ deptL =>
      Future.sequence(deptL.map{ dept =>
        empF(dept._1).flatMap{ empL =>
          Future.sequence(empL.map{ emp =>
            salaryF(emp.empNo).map{ salary =>
              DeptEmployeeStat(
                deptNo = dept._1,
                deptName = dept._2,
                totalEmployees = dept._3,
                employee = emp,
                salary = salary
              )
            }
          })
        }
      })
    }.map{ i =>
      Ok(Json.toJson(i.flatten))
    }
  }

  def sync = Action { request =>
    val result = DB.withConnection{ implicit c =>
      for{
        dept <- sqlForDeptPopulation().as(deptPopulationParser.*)
        emp <- sqlForLastNEmployeeByDept(dept._1, 10).as(employeeParser.*)
      } yield {
        DeptEmployeeStat(
          deptNo = dept._1,
          deptName = dept._2,
          totalEmployees = dept._3,
          employee = emp,
          salary = sqlForAvgSalary(emp.empNo).as(salaryParser.single)
        )
      }
    }
    Ok(Json.toJson(result))
  }
}
